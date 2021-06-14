package pw.komarov.streamer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@SuppressWarnings({"WeakerAccess","unused","UnusedReturnValue"})
public class Streamer<T> implements Stream<T>, Iterable<T> {
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

    private final List<Runnable> onCloseSequences = new LinkedList<>();

    @Override
    public void close() {
        if (state == State.WAITING)
            internalClose();

        //completing onClose sequences
        for (Iterator<Runnable> iterator = onCloseSequences.iterator(); iterator.hasNext(); ) {
            Runnable runnable = iterator.next();
            try {
                runnable.run();
            } catch (Exception ignored) {} finally {
                iterator.remove();
            }
        }
    }

    private void internalClose() {
        externalIterator = null;
        state = State.CLOSED;
    }

    private void throwIfNotWaiting() {
        if (state != State.WAITING)
            throw new IllegalStateException("stream has already been operated upon or closed");
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        throwIfNotWaiting();

        onCloseSequences.add(closeHandler);

        return this;
    }

    /*
            Intermediate methods (conveyor/pipeline)
    */

    @Override
    public Stream<T> limit(long maxSize) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> skip(long n) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> distinct() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> sorted() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        throw new UnsupportedOperationException("will be soon");
    }

    /*
            Terminal methods
    */

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> findFirst() {
        return findAny();
    }

    @Override
    public Optional<T> findAny() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
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
        forEach(action); //опять же мы упорядочены родительским источником, поэтому, в нашем случае forEach и forEachOrdered эквивалентны
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.ORDERED); //создадим сплитератор на основе предоставляемого нами итератора
    }

    @Override
    public Stream<T> unordered() {
        return this; //так же можно вернуть себя
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