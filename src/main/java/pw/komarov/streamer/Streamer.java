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

        private void calcNextAndHasNext() { //метод расчитывающий внутренние, закрытые поля next и hasNext на основании расчитанного опционала
            Optional<T> opt = getNext(intermediateOperations);

            //noinspection OptionalAssignedToNull
            hasNext = opt != null;
            if (hasNext)
                next = opt.orElse(null);
        }

        @SuppressWarnings("unchecked")
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

        return this;
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);

        throwIfNotWaiting();

        intermediateOperations.add(new SortedOperation<>(comparator));

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
    private static class FlatMapOperation<T, R> implements IntermediateOperation {
        private final Function<? super T, ? extends Stream<? extends R>> function;

        FlatMapOperation(Function<? super T, ? extends Stream<? extends R>> function) {
            this.function = function;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        throwIfNotWaiting();

        intermediateOperations.add(new FlatMapOperation<>(mapper));

        return (Streamer<R>) this;
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
        throwIfNotWaiting();

        state = State.OPERATED;

        return streamerIterator;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        throwIfNotWaiting();

        state = State.OPERATED;

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
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        throwIfNotWaiting();

        state = State.OPERATED;

        while (streamerIterator.hasNext())
            action.accept(streamerIterator.next());
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public long count() {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Object[] toArray() {
        throwIfNotWaiting();

        state = State.OPERATED;

        //todo: терминальные операции...

        internalClose();

        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        throwIfNotWaiting();

        state = State.OPERATED;

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
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<T> parallel() {
        throw new UnsupportedOperationException();
    }
}