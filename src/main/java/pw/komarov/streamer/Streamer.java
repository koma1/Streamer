package pw.komarov.streamer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@SuppressWarnings({"WeakerAccess","unused","UnusedReturnValue"})
public final class Streamer<T> implements Stream<T>, Iterable<T> {
    private Iterator<T> externalIterator; //source of data

    /*
            Constructing
    */

    private Streamer(Iterator<T> externalIterator) {
        this.externalIterator = externalIterator;
    }

    public static <T> Streamer<T> empty() {
        return
                new Streamer<>(new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public T next() {
                        throw new NoSuchElementException();
                    }
                });
    }

    @SafeVarargs
    public static <E> Streamer<E> of(E... args) {
        return of(Arrays.asList(args));
    }

    public static <E> Streamer<E> of(Iterable<E> iterable) {
        return of(iterable.iterator());
    }

    public static <E> Streamer<E> of(Iterator<E> iterator) {
        return new Streamer<>(iterator);
    }

    private static abstract class AbstractInfiniteIterator<E> implements Iterator<E> {
        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            throw new UnsupportedOperationException();
        }
    }

    private static class InfiniteGenerator<E> extends AbstractInfiniteIterator<E> {
        private final Supplier<E> supplier;

        InfiniteGenerator(Supplier<E> supplier) {
            this.supplier = supplier;
        }

        @Override
        public E next() {
            return supplier.get();
        }
    }

    public static <E> Streamer<E> generate(Supplier<E> supplier) {
        return of(new InfiniteGenerator<>(supplier));
    }

    public static class InfiniteIterator<E> extends AbstractInfiniteIterator<E> {
        private E value; //previous value (at first call - initial value)
        private final UnaryOperator<E> unaryOperator;

        InfiniteIterator(E initial, UnaryOperator<E> unaryOperator) {
            this.value = initial;
            this.unaryOperator = unaryOperator;
        }

        @Override
        public E next() {
            E prev = this.value; //store previous value
            this.value = unaryOperator.apply(prev);

            return prev;
        }
    }

    public static <E> Streamer<E> iterate(E initial, UnaryOperator<E> unaryOperator) {
        return of(new InfiniteIterator<>(initial, unaryOperator));
    }

    /*
            Closure
    */

    private enum State {WAITING, OPERATED, CLOSED}

    private State state = State.WAITING;

    @Override
    public void close() {
        if (state == State.WAITING)
            internalClose();

        //completing onClose sequences
        RuntimeException rte = null;
        for (Iterator<Runnable> iterator = onCloseSequences.iterator(); iterator.hasNext(); ) {
            Runnable runnable = iterator.next();
            try {
                runnable.run();
            } catch (RuntimeException e) {
                if (rte == null) //если это первое исключение в цепочке...
                    rte = e; //...сохраним его
                else //если не первое...
                    rte.addSuppressed(e); //...сохраним его в suppressed первого
            } finally {
                iterator.remove();
            }
        }

        if (rte != null)
            throw rte;
    }

    private void internalClose() {
        externalIterator = null;

        state = State.CLOSED;
    }

    private void throwIfNotWaitingOrSetOperated() {
        throwIfNotWaiting();

        state = State.OPERATED;
    }

    private void throwIfNotWaiting() {
        if (state != State.WAITING)
            throw new IllegalStateException("stream has already been operated upon or closed");
    }

    /*
            Internal streamer iterator
    */

    private final StreamerIterator streamerIterator = new StreamerIterator();

    private class StreamerIterator implements Iterator<T> {
        private Boolean hasNext;
        private T next;

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                if (sortedCount > 0)
                    calculateSorted();

                calcNextAndHasNext();

                if (!hasNext && state != State.CLOSED)
                    internalClose();
            }

            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();

            hasNext = null;

            return next;
        }

        @SuppressWarnings({"OptionalAssignedToNull","unchecked"})
        private void calculateSorted() {
            for (int i = 1; i <= sortedCount; i++) {
                //building local operations list (from general operations list, by extracting sublist)
                final List<IntermediateOperation> localOperations = new LinkedList<>();
                SortedOperation<T> sortedOperation = null;
                for (Iterator<IntermediateOperation> itr = intermediateOperations.iterator(); itr.hasNext(); ) {
                    IntermediateOperation operation = itr.next();
                    try {
                        if (operation instanceof SortedOperation) {
                            sortedOperation = (SortedOperation<T>) operation;
                            break;
                        } else
                            localOperations.add(operation);
                    } finally {
                        itr.remove();
                    }
                }

                //data collecting
                final List<T> data = new ArrayList<>();
                Optional<T> nextOpt;
                do {
                    nextOpt = getNext(localOperations);
                    if (nextOpt != null)
                        data.add(nextOpt.orElse(null));

                } while (nextOpt != null);

                //sorting...
                if (sortedOperation != null)
                    data.sort(sortedOperation.comparator);

                //now, we can replace the iterator
                externalIterator = data.iterator();
            }
        }

        private void calcNextAndHasNext() { //метод расчитывающий внутренние, закрытые поля next и hasNext на основании расчитанного опционала
            Optional<T> opt = getNext(intermediateOperations);

            //noinspection OptionalAssignedToNull
            hasNext = opt != null;
            if (hasNext)
                next = opt.orElse(null);
        }

        @SuppressWarnings({"unchecked"})
        private Optional<T> getNext(List<IntermediateOperation> operations) {
            T next = null;
            boolean terminated = false;

            boolean hasNext = externalIterator.hasNext();
            while (hasNext && !terminated) {
                next = externalIterator.next();

                boolean filtered = false;
                for (IntermediateOperation operation : operations)
                    if (operation instanceof FilteringOperation) {
                        if (!filtered) {
                            filtered = ((FilteringOperation) operation).test(next);
                            if (filtered && operation instanceof LimitOperation)
                                terminated = true;
                        }
                    } else if (operation instanceof MapOperation)
                        next = (T) ((MapOperation)operation).function.apply(next);
                    else
                        throw new UnsupportedOperationException("getNext(): " + operation.getClass().getSimpleName());

                if (!filtered)
                    break;
                else
                    hasNext = externalIterator.hasNext();
            }

            if (hasNext && !terminated) {
                for (Consumer<? super T> peekSequence : peekSequences)
                    peekSequence.accept(next);

                return Optional.ofNullable(next);
            }

            //noinspection OptionalAssignedToNull
            return null;
        }
    }

    /*
            Intermediate methods (conveyor/pipeline)
    */

    private interface IntermediateOperation {}

    private final List<IntermediateOperation> intermediateOperations = new LinkedList<>();

    private interface FilteringOperation<T> extends IntermediateOperation, Predicate<T> {}

    //limit()
    private static class LimitOperation<E> implements FilteringOperation<E> {
        private long filteredByLimit; //filtered elements count by Limit operation

        private final long maxSize; //maximum elements count that stream can return

        LimitOperation(long maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public boolean test(E t) {
            return maxSize < ++filteredByLimit;
        }
    }

    @Override
    public Stream<T> limit(long maxSize) {
        throwIfNotWaiting();

        intermediateOperations.add(new LimitOperation(maxSize));

        return this;
    }

    //skip()
    private static class SkipOperation implements FilteringOperation {
        private final long totalCount; //Total elements count, that streamer must skip
        private long processedCount; //elements count that streamer was skipped

        SkipOperation(long totalCount) {
            this.totalCount = totalCount;
        }

        @Override
        public boolean test(Object o) {
            return processedCount++ < totalCount;
        }
    }

    @Override
    public Stream<T> skip(long n) {
        throwIfNotWaiting();

        intermediateOperations.add(new SkipOperation(n));

        return this;
    }

    //distinct()
    private static class DistinctOperation implements FilteringOperation {
        private Set<Object> objects = new HashSet<>();

        @Override
        public boolean test(Object o) {
            return !objects.add(o);
        }
    }

    @Override
    public Stream<T> distinct() {
        throwIfNotWaiting();

        intermediateOperations.add(new DistinctOperation());

        return this;
    }

    //filter()
    private static class FilterOperation<T> implements FilteringOperation<T> {
        private final Predicate<? super T> predicate;

        public FilterOperation(Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean test(T t) {
            return !predicate.test(t);
        }
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        throwIfNotWaiting();

        intermediateOperations.add(new FilterOperation<>(predicate));

        return this;
    }

    //sorted()
    private int sortedCount;

    public static class SortedOperation<T> implements IntermediateOperation {
        private final Comparator<? super T> comparator;

        public SortedOperation(Comparator<? super T> comparator) {
            this.comparator = comparator;
        }
    }

    @Override
    public Stream<T> sorted() {
        throwIfNotWaiting();

        intermediateOperations.add(new SortedOperation<>(null));
        sortedCount++;

        return this;
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);

        throwIfNotWaiting();

        intermediateOperations.add(new SortedOperation<>(comparator));
        sortedCount++;

        return this;
    }

    //map()
    private static class MapOperation<T, R> implements IntermediateOperation {
        private final Function<? super T, ? extends R> function;

        MapOperation(Function<? super T, ? extends R> function) {
            this.function = function;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        throwIfNotWaiting();

        intermediateOperations.add(new MapOperation<>(mapper));

        return (Streamer<R>) this;
    }

    //flatMap()
    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);

        class IteratorOfR implements Iterator<R> {
            private final Iterator<T> OfT = Streamer.this.iterator(); //родительский итератор (содержит элементы множества)

            private Iterator<? extends R> ofR; //элементы подмножества Stream<R> которые будем возвращать конечному клиенту

            @Override
            public R next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofR.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofR == null || !ofR.hasNext()) && OfT.hasNext()) //если ofR не задан (напр.: первый запрос), или в ofR отсутствуют элементы и при этом есть что раскладывать в родительском (ofT)...
                    ofR = mapper.apply(OfT.next()).iterator(); //...разложим элемент из ofT в подмножество ofR

                return ofR != null && ofR.hasNext();
            }
        }

        return Streamer.of(new IteratorOfR());
    }

    //mapToInt()
    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);

        class OfInt implements PrimitiveIterator.OfInt {
            private final ToIntFunction<? super T> mapper;

            public OfInt(ToIntFunction<? super T> mapper) {
                this.mapper = mapper;
            }

            @Override
            public int nextInt() {
                return mapper.applyAsInt(streamerIterator.next());
            }

            @Override
            public boolean hasNext() {
                return streamerIterator.hasNext();
            }
        }

        return StreamSupport
                .intStream(
                        Spliterators.spliteratorUnknownSize(
                                new OfInt(mapper),
                                0),
                        false);
    }

    //mapToLong()
    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);

        class OfLong implements PrimitiveIterator.OfLong {
            private final ToLongFunction<? super T> mapper;

            public OfLong(ToLongFunction<? super T> mapper) {
                this.mapper = mapper;
            }

            @Override
            public long nextLong() {
                return mapper.applyAsLong(streamerIterator.next());
            }

            @Override
            public boolean hasNext() {
                return streamerIterator.hasNext();
            }
        }

        return StreamSupport
                .longStream(
                        Spliterators.spliteratorUnknownSize(
                                new OfLong(mapper),
                                0),
                        false);
    }

    //mapToDouble()
    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);

        class OfDouble implements PrimitiveIterator.OfDouble {
            private final ToDoubleFunction<? super T> mapper;

            public OfDouble(ToDoubleFunction<? super T> mapper) {
                this.mapper = mapper;
            }

            @Override
            public double nextDouble() {
                return mapper.applyAsDouble(streamerIterator.next());
            }

            @Override
            public boolean hasNext() {
                return streamerIterator.hasNext();
            }
        }

        return StreamSupport
                .doubleStream(
                        Spliterators.spliteratorUnknownSize(
                                new OfDouble(mapper),
                                0),
                        false);
    }

    //flatMapToInt()
    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);

        class OfInt implements PrimitiveIterator.OfInt {
            private final Iterator<T> iteratorOfT;
            private final Function<? super T, ? extends IntStream> mapper;

            private PrimitiveIterator.OfInt ofInt;

            public OfInt(Iterator<T> iteratorOfT, Function<? super T, ? extends IntStream> mapper) {
                this.iteratorOfT = iteratorOfT;
                this.mapper = mapper;
            }

            @Override
            public int nextInt() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofInt.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofInt == null || !ofInt.hasNext()) && iteratorOfT.hasNext())
                    ofInt = mapper.apply(iteratorOfT.next()).iterator();

                return ofInt != null && ofInt.hasNext();
            }
        }

        return StreamSupport.intStream(
                Spliterators.spliteratorUnknownSize(new OfInt(this.iterator(), mapper), 0),
                false
        );
    }

    //flatMapToLong()
    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);

        class OfLong implements PrimitiveIterator.OfLong {
            private final Iterator<T> iteratorOfT;
            private final Function<? super T, ? extends LongStream> mapper;

            private PrimitiveIterator.OfLong ofLong;

            public OfLong(Iterator<T> iteratorOfT, Function<? super T, ? extends LongStream> mapper) {
                this.iteratorOfT = iteratorOfT;
                this.mapper = mapper;
            }

            @Override
            public long nextLong() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofLong.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofLong == null || !ofLong.hasNext()) && iteratorOfT.hasNext())
                    ofLong = mapper.apply(iteratorOfT.next()).iterator();

                return ofLong != null && ofLong.hasNext();
            }
        }

        return StreamSupport.longStream(
                Spliterators.spliteratorUnknownSize(new OfLong(this.iterator(), mapper), 0),
                false
        );
    }

    //flatMapToDouble()
    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);

        class OfDouble implements PrimitiveIterator.OfDouble {
            private final Iterator<T> iteratorOfT;
            private final Function<? super T, ? extends DoubleStream> mapper;

            private PrimitiveIterator.OfDouble ofDouble;

            public OfDouble(Iterator<T> iteratorOfT, Function<? super T, ? extends DoubleStream> mapper) {
                this.iteratorOfT = iteratorOfT;
                this.mapper = mapper;
            }

            @Override
            public double nextDouble() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofDouble.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofDouble == null || !ofDouble.hasNext()) && iteratorOfT.hasNext())
                    ofDouble = mapper.apply(iteratorOfT.next()).iterator();

                return ofDouble != null && ofDouble.hasNext();
            }
        }

        return StreamSupport.doubleStream(
                Spliterators.spliteratorUnknownSize(new OfDouble(this.iterator(), mapper), 0),
                false
        );
    }

    //peek()
    private final List<Consumer<? super T>> peekSequences = new LinkedList<>();

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        throwIfNotWaiting();

        peekSequences.add(action);

        return this;
    }

    //onClose()
    private final List<Runnable> onCloseSequences = new LinkedList<>();

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        throwIfNotWaiting();

        onCloseSequences.add(closeHandler);

        return this;
    }

    /*
            Terminal methods
    */

    @Override
    public Iterator<T> iterator() {
        throwIfNotWaitingOrSetOperated();

        return streamerIterator;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> findFirst() {
        return findAny();
    }

    @Override
    public Optional<T> findAny() {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        throwIfNotWaitingOrSetOperated();

        while (streamerIterator.hasNext())
            action.accept(streamerIterator.next());
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public long count() {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Object[] toArray() {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        throwIfNotWaitingOrSetOperated();

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean isParallel() {
        return false; //мы не поддерживаем параллелизм
    }

    @Override
    public Stream<T> sequential() {
        return this; //мы "последовательны", поэтому вернем себя же
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.ORDERED);
    }

    @Override
    public Stream<T> unordered() {
        return this;
    }

    /*
            Unsupported
    */

    @Override
    public Stream<T> parallel() {
        throw new UnsupportedOperationException();
    }
}