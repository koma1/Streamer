package pw.komarov.streams;

import pw.komarov.utils.NullableValue;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public final class Streamer<T> implements Stream<T>, Iterable<T> {
    /*
            Constructing
    */

    private InternalStreamerIterator streamerIterator;
    private Iterable<T> sourceIterable;

    private Streamer(Iterable<T> iterable) {
        this.sourceIterable = iterable;
    }

    private Streamer(Iterator<T> sourceIterator) {
        this.streamerIterator = new InternalStreamerIterator(sourceIterator);
    }

    @SuppressWarnings("WeakerAccess")
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
        return from(Arrays.asList(args));
    }

    @SuppressWarnings("WeakerAccess")
    public static <E> Streamer<E> from(Iterable<E> iterable) {
        return new Streamer<>(iterable);
    }

    @SuppressWarnings("WeakerAccess")
    public static <E> Streamer<E> from(Iterator<E> iterator) {
        return new Streamer<>(iterator);
    }

    @SuppressWarnings("unused")
    public static <E> Streamer<E> from(Stream<E> stream) {
        return new Streamer<>(stream.iterator());
    }

    @SuppressWarnings("WeakerAccess")
    public static Streamer<Integer> from(IntStream intStream) {
        return new Streamer<>(intStream.boxed().iterator());
    }

    @SuppressWarnings("unused")
    public static Streamer<Long> from(LongStream longStream) {
        return new Streamer<>(longStream.boxed().iterator());
    }

    @SuppressWarnings("unused")
    public static Streamer<Double> from(DoubleStream doubleStream) {
        return new Streamer<>(doubleStream.boxed().iterator());
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static Streamer<Integer> from(CharSequence charSequence) {
        return from(charSequence.chars());
    }

    @SuppressWarnings("WeakerAccess")
    public static <K,V> Streamer<Map.Entry<K,V>> from(Map<K,V> map) {
        return from(map.entrySet());
    }

    @SuppressWarnings("WeakerAccess")
    public static <K> Streamer<K> fromMapKeys(Map<K,?> map) {
        return from(map.keySet());
    }

    @SuppressWarnings("WeakerAccess")
    public static <V> Streamer<V> fromMapValues(Map<?,V> map) {
        return from(map.values());
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
        return from(new InfiniteGenerator<>(supplier));
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

    @SuppressWarnings("WeakerAccess")
    public static <E> Streamer<E> iterate(E initial, UnaryOperator<E> unaryOperator) {
        return from(new InfiniteIterator<>(initial, unaryOperator));
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
                if (rte == null) //if it first exception
                    rte = e; //...save it
                else //if not first...
                    rte.addSuppressed(e); //...save it in suppressed
            } finally {
                iterator.remove();
            }
        }

        if (rte != null)
            throw rte;
    }

    private void internalClose() {
        streamerIterator.setSourceIterator(null);

        state = State.CLOSED;
    }

    private void throwIfNotWaiting() {
        if (state != State.WAITING)
            throw new IllegalStateException("stream has already been operated upon or closed");
    }

    private void prepareRun() {
        throwIfNotWaiting();

        state = State.OPERATED;

        //preparing delayed run from Iterable<> source
        if (streamerIterator == null && sourceIterable != null) {
            streamerIterator = new InternalStreamerIterator(sourceIterable.iterator());
            sourceIterable = null;
        }
    }

    /*
            Internal streams iterator
    */

    private class InternalStreamerIterator implements Iterator<T> {
        private Iterator<T> sourceIterator; //source of data

        InternalStreamerIterator(Iterator<T> sourceIterator) {
            this.sourceIterator = sourceIterator;
        }

        private boolean noNext;

        private Boolean hasNext;
        private T next;

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                if (collectedOperationsCount > 0) {
                    calculateCollectedOperations();
                    collectedOperationsCount = collectedOperationsCount *(-1);
                }

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

        @SuppressWarnings("unchecked")
        private void calculateCollectedOperations() {
            for (int i = 1; i <= collectedOperationsCount; i++) {
                //building local operations list (from general operations list, by extracting sublist)
                final List<IntermediateOperation> localOperations = new LinkedList<>();
                CollectedOperation collectedOperation = null;
                for (Iterator<IntermediateOperation> itr = intermediateOperations.iterator(); itr.hasNext(); ) {
                    IntermediateOperation operation = itr.next();
                    try {
                        if (operation instanceof CollectedOperation) {
                            collectedOperation = (CollectedOperation) operation;
                            break;
                        } else
                            localOperations.add(operation);
                    } finally {
                        itr.remove();
                    }
                }

                //data collecting
                List<T> data = new ArrayList<>();
                NullableValue<T> nextValue;
                do {
                    nextValue = getNext(localOperations);
                    if (nextValue != null)
                        data.add(nextValue.get());

                } while (nextValue != null);

                //sorting...
                if (collectedOperation != null)
                    if (collectedOperation instanceof ReversedOperation)
                        Collections.reverse(data);
                    else if (collectedOperation instanceof SortedOperation)
                        data.sort(((SortedOperation<T>)collectedOperation).comparator);
                    else if (collectedOperation.getClass() == LastOperation.class) { //todo: getClass -> instanceof
                        int fromIndex = data.size() - ((LastOperation) collectedOperation).count;
                        if (fromIndex >= 0)
                            data = data.subList(fromIndex, data.size());
                    } else
                        throw new UnsupportedOperationException("calculateCollectedOperations() - unknown CollectedOperation class: " + collectedOperation.getClass());

                //now, we can replace the iterator
                setSourceIterator(data.iterator());
            }
        }

        private void calcNextAndHasNext() { //calculating next and getNext
            NullableValue<T> nextValue = getNext(intermediateOperations);

            hasNext = nextValue != null;
            if (hasNext)
                next = nextValue.get();
        }

        @SuppressWarnings({"unchecked"})
        private NullableValue<T> getNext(List<IntermediateOperation> operations) {
            T next = null;

            boolean hasNext = !noNext && sourceIterator.hasNext();
            while (hasNext) {
                next = sourceIterator.next();

                boolean filtered = false;
                for (IntermediateOperation operation : operations)
                    if (operation instanceof FilteringOperation) {
                        if (!filtered) {
                            filtered = ((FilteringOperation) operation).test(next);
                            if (filtered && operation instanceof LimitOperation) {
                                filtered = false;
                                noNext = true;
                            }
                        }
                    } else if (operation instanceof MapOperation)
                        next = (T) ((MapOperation)operation).function.apply(next);
                    else
                        throw new UnsupportedOperationException("getNext(): " + operation.getClass().getSimpleName());

                if (!filtered)
                    break;
                else
                    hasNext = sourceIterator.hasNext();
            }

            if (hasNext) {
                for (Consumer<? super T> peekSequence : peekSequences)
                    peekSequence.accept(next);

                return NullableValue.of(next);
            }

            return null;
        }

        void setSourceIterator(Iterator<T> sourceIterator) {
            this.sourceIterator = sourceIterator;

            noNext = false;
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
            return maxSize < ++filteredByLimit + 1;
        }
    }

    @Override
    public Streamer<T> limit(long maxSize) {
        throwIfNotWaiting();

        intermediateOperations.add(new LimitOperation(maxSize));

        return this;
    }

    //skip()
    private static class SkipOperation implements FilteringOperation {
        private final long totalCount; //Total elements count, that streams must skip
        private long processedCount; //elements count that streams was skipped

        SkipOperation(long totalCount) {
            this.totalCount = totalCount;
        }

        @Override
        public boolean test(Object o) {
            return processedCount++ < totalCount;
        }
    }

    @Override
    public Streamer<T> skip(long n) {
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
    public Streamer<T> distinct() {
        throwIfNotWaiting();

        intermediateOperations.add(new DistinctOperation());

        return this;
    }

    //filter()
    private static class FilterOperation<T> implements FilteringOperation<T> {
        private final Predicate<? super T> predicate;

        FilterOperation(Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean test(T t) {
            return !predicate.test(t);
        }
    }

    @Override
    public Streamer<T> filter(Predicate<? super T> predicate) {
        throwIfNotWaiting();

        intermediateOperations.add(new FilterOperation<>(predicate));

        return this;
    }

    private int collectedOperationsCount;

    private interface CollectedOperation extends IntermediateOperation {}

    //sorted()

    private class SortedOperation<E> implements CollectedOperation {
        private final Comparator<? super E> comparator;

        SortedOperation(Comparator<? super E> comparator) {
            this.comparator = comparator;
            collectedOperationsCount++;
        }
    }

    @Override
    public Streamer<T> sorted() {
        throwIfNotWaiting();

        intermediateOperations.add(new SortedOperation<>(null));

        return this;
    }

    @Override
    public Streamer<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);

        throwIfNotWaiting();

        intermediateOperations.add(new SortedOperation<>(comparator));

        return this;
    }

    private class ReversedOperation<E> extends SortedOperation<E> {
        ReversedOperation() {
            super(null);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Streamer<T> reversed() {
        throwIfNotWaiting();

        intermediateOperations.add(new ReversedOperation());

        return this;
    }

    //last()
    private class LastOperation implements CollectedOperation {
        private final int count; //Total elements count, that stream must take from end

        LastOperation(int count) {
            this.count = count;
            collectedOperationsCount++;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Streamer<T> last(int count) {
        throwIfNotWaiting();

        intermediateOperations.add(new LastOperation(count));

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
    public <R> Streamer<R> map(Function<? super T, ? extends R> mapper) {
        throwIfNotWaiting();

        intermediateOperations.add(new MapOperation<>(mapper));

        return (Streamer<R>)Streamer.from(this.iterator());
    }

    //flatMap()
    @Override
    public <R> Streamer<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);

        class IteratorOfR implements Iterator<R> {
            private final Iterator<T> ofT = Streamer.this.iterator(); //parent iterator

            private Iterator<? extends R> ofR; //SubElements

            @Override
            public R next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofR.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofR == null || !ofR.hasNext()) && ofT.hasNext()) //if ofR not set (ex: first req) or ofR empty and ofT it have...
                    ofR = mapper.apply(ofT.next()).iterator(); //divide ofT...

                return ofR != null && ofR.hasNext();
            }
        }

        return Streamer.from(new IteratorOfR());
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);

        class OfInt implements PrimitiveIterator.OfInt {
            private final Iterator<T> ofT = Streamer.this.iterator();

            @Override
            public int nextInt() {
                return mapper.applyAsInt(ofT.next());
            }

            @Override
            public boolean hasNext() {
                return ofT.hasNext();
            }
        }

        return StreamSupport
                .intStream(
                        Spliterators.spliteratorUnknownSize(
                                new OfInt(),
                                0),
                        false);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);

        class OfLong implements PrimitiveIterator.OfLong {
            private final Iterator<T> ofT = Streamer.this.iterator();

            @Override
            public long nextLong() {
                return mapper.applyAsLong(ofT.next());
            }

            @Override
            public boolean hasNext() {
                return ofT.hasNext();
            }
        }

        return StreamSupport
                .longStream(
                        Spliterators.spliteratorUnknownSize(
                                new OfLong(),
                                0),
                        false);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);

        class OfDouble implements PrimitiveIterator.OfDouble {
            private final Iterator<T> ofT = Streamer.this.iterator();

            @Override
            public double nextDouble() {
                return mapper.applyAsDouble(ofT.next());
            }

            @Override
            public boolean hasNext() {
                return ofT.hasNext();
            }
        }

        return StreamSupport
                .doubleStream(
                        Spliterators.spliteratorUnknownSize(
                                new OfDouble(),
                                0),
                        false);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);

        class OfInt implements PrimitiveIterator.OfInt {
            private final Iterator<T> ofT = Streamer.this.iterator();

            private PrimitiveIterator.OfInt ofInt;

            @Override
            public int nextInt() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofInt.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofInt == null || !ofInt.hasNext()) && ofT.hasNext())
                    ofInt = mapper.apply(ofT.next()).iterator();

                return ofInt != null && ofInt.hasNext();
            }
        }

        return StreamSupport.intStream(
                Spliterators.spliteratorUnknownSize(new OfInt(), 0),
                false
        );
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);

        class OfLong implements PrimitiveIterator.OfLong {
            private final Iterator<T> ofT = Streamer.this.iterator();

            private PrimitiveIterator.OfLong ofLong;

            @Override
            public long nextLong() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofLong.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofLong == null || !ofLong.hasNext()) && ofT.hasNext())
                    ofLong = mapper.apply(ofT.next()).iterator();

                return ofLong != null && ofLong.hasNext();
            }
        }

        return StreamSupport.longStream(
                Spliterators.spliteratorUnknownSize(new OfLong(), 0),
                false
        );
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);

        class OfDouble implements PrimitiveIterator.OfDouble {
            private final Iterator<T> ofT = Streamer.this.iterator();

            private PrimitiveIterator.OfDouble ofDouble;

            @Override
            public double nextDouble() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return ofDouble.next();
            }

            @Override
            public boolean hasNext() {
                while ((ofDouble == null || !ofDouble.hasNext()) && ofT.hasNext())
                    ofDouble = mapper.apply(ofT.next()).iterator();

                return ofDouble != null && ofDouble.hasNext();
            }
        }

        return StreamSupport.doubleStream(
                Spliterators.spliteratorUnknownSize(new OfDouble(), 0),
                false
        );
    }

    //peek()
    private final List<Consumer<? super T>> peekSequences = new LinkedList<>();

    @Override
    public Streamer<T> peek(Consumer<? super T> action) {
        throwIfNotWaiting();

        peekSequences.add(action);

        return this;
    }

    //onClose()
    private final List<Runnable> onCloseSequences = new LinkedList<>();

    @Override
    public Streamer<T> onClose(Runnable closeHandler) {
        throwIfNotWaiting();

        onCloseSequences.add(closeHandler);

        return this;
    }

    /*
            Terminal methods
    */

    private List<T> finishToList() {
        List<T> result = new ArrayList<>();

        while (streamerIterator.hasNext())
            result.add(streamerIterator.next());

        return result;
    }

    @Override
    public Iterator<T> iterator() {
        prepareRun();

        return streamerIterator;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);

        prepareRun();

        try {
            while (streamerIterator.hasNext())
                if (predicate.test(streamerIterator.next()))
                    return true;

            return false;
        } finally {
            internalClose();
        }
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);

        prepareRun();

        try {
            while (streamerIterator.hasNext())
                if (!predicate.test(streamerIterator.next()))
                    return false;

            return true;
        } finally {
            internalClose();
        }
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        prepareRun();

        try {
            while (streamerIterator.hasNext())
                if (predicate.test(streamerIterator.next()))
                    return false;

            return true;
        } finally {
            internalClose();
        }
    }

    @Override
    public Optional<T> findFirst() {
        return findAny();
    }

    @Override
    public Optional<T> findAny() {
        prepareRun();

        try {
            if (streamerIterator.hasNext())
                return Optional.of(streamerIterator.next());
            else
                return Optional.empty();
        } finally {
            internalClose();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Optional<T> findLast() {
        prepareRun();

        try {
            if (!streamerIterator.hasNext())
                return Optional.empty();

            T last;

            do {
                last = streamerIterator.next();
            } while (streamerIterator.hasNext());

            return Optional.of(last);
        } finally {
            internalClose();
        }
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        prepareRun();

        try {
            while (streamerIterator.hasNext())
                action.accept(streamerIterator.next());
        } finally {
            internalClose();
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        forEach(action);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);

        prepareRun();

        try {
            List<T> list = finishToList();
            list.sort(comparator);

            return !list.isEmpty() ? Optional.of(list.get(0)) : Optional.empty();
        } finally {
            internalClose();
        }
    }

    @SuppressWarnings("RedundantComparatorComparing")
    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);

        return min(comparator.reversed());
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        Objects.requireNonNull(accumulator);

        prepareRun();

        try {
            if (!streamerIterator.hasNext())
                return identity;

            T value = identity;
            while (streamerIterator.hasNext())
                value = accumulator.apply(value, streamerIterator.next());

            return value;
        } finally {
            internalClose();
        }
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> binaryOperator) {
        Objects.requireNonNull(binaryOperator);

        prepareRun();

        try {
            if (!streamerIterator.hasNext())
                return Optional.empty();

            T value = streamerIterator.next();
            while (streamerIterator.hasNext())
                value = binaryOperator.apply(value, streamerIterator.next());

            return Optional.of(value);
        } finally {
            internalClose();
        }
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);

        prepareRun();

        try {
            if (!streamerIterator.hasNext())
                return identity;

            U valueU = identity;
            while (streamerIterator.hasNext())
                valueU = accumulator.apply(valueU, streamerIterator.next());

            return valueU;
        } finally {
            internalClose();
        }
    }

    @Override
    public long count() {
        prepareRun();

        try {
            int count = 0;

            for (; streamerIterator.hasNext(); streamerIterator.next())
                count++;

            return count;
        } finally {
            internalClose();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(supplier);

        prepareRun();

        try {
            R result = supplier.get();

            for (;streamerIterator.hasNext();)
                accumulator.accept(result, streamerIterator.next());

            return result;
        } finally {
            internalClose();
        }
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        Objects.requireNonNull(collector);

        prepareRun();

        try {
            A accumulator = collector.supplier().get();

            while (streamerIterator.hasNext())
                collector.accumulator().accept(accumulator, streamerIterator.next());

            return collector.finisher().apply(accumulator);
        } finally {
            internalClose();
        }
    }

    @Override
    public Object[] toArray() {
        prepareRun();

        try {
            return finishToList().toArray();
        } finally {
            internalClose();
        }
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        Objects.requireNonNull(generator);

        prepareRun();

        try {
            List<T> list = finishToList();

            A[] result = generator.apply(list.size());

            if (result.length < list.size())
                throw new IndexOutOfBoundsException("does not fit");

            list.toArray(result);

            return result;
        } finally {
            internalClose();
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.ORDERED);
    }

    /*
            Other methods
    */

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public Streamer<T> sequential() {
        return this;
    }

    @Override
    public Streamer<T> unordered() {
        return this;
    }

    @Override
    public Streamer<T> parallel() {
        throw new UnsupportedOperationException();
    }

    /*
            Additional
    */

    @SuppressWarnings("WeakerAccess")
    public <K> Map<K,Collection<T>> groupBy(Function<? super T,? extends K> groupMapper) {
        return collect(HashMap::new,
                (map, object) -> map.merge(
                        groupMapper.apply(object),
                        new ArrayList<>(Collections.singletonList(object)),
                        ((left, right) -> {
                            left.addAll(right);
                            return left;
                        }) ),
                null);
    }
}