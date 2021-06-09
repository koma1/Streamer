package pw.komarov.streamer;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.*;

public class Streamer<T> implements Stream<T> {
    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        throw new UnsupportedOperationException("will be soon");
    }

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
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        throw new UnsupportedOperationException("will be soon");
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
    public Stream<T> distinct() {
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
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<T> limit(long maxSize) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> skip(long n) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        throw new UnsupportedOperationException();
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
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
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
    public long count() {
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
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Optional<T> findAny() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Spliterator<T> spliterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isParallel() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> sequential() {
        throw new UnsupportedOperationException("will be soon");
    }

    @Override
    public Stream<T> parallel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<T> unordered() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("will be soon");
    }
}