package pw.komarov.java.lang;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class NullableOptional<T> {
    private static final NullableOptional<?> EMPTY      = new NullableOptional<>(null);
    private static final NullableOptional<?> NULLABLE   = new NullableOptional<>(null);

    private final T value;

    /*
        Constructing
    */

    private NullableOptional(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <E> NullableOptional<E> empty() {
        return (NullableOptional<E>) EMPTY;
    }

    public static <E> NullableOptional<E> of(E value) {
        Objects.requireNonNull(value);

        return ofNullable(value);
    }

    public static <E> NullableOptional<E> ofNullable(E value) {
        return value == null ? empty() : new NullableOptional<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <E> NullableOptional<E> ofNullableNotEmpty(E value) {
        return value == null ? (NullableOptional<E>) NULLABLE : new NullableOptional<>(value);
    }

    /*

    */

    public boolean isPresent() {
        return this != EMPTY;
    }

    public T get() {
        if (!isPresent())
            throw new NoSuchElementException("No value present");

        return value;
    }

    public T orElse(T value) {
        return isPresent() ? this.value : value;
    }

    public <THROWABLE extends Throwable> T orElseThrow(Supplier<? extends THROWABLE> throwableSupplier) throws THROWABLE {
        if (!isPresent())
            throw throwableSupplier.get();

        return value;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        if (isPresent())
            return get();
        else
            return supplier.get();
    }

    public ElseIf ifPresent(Consumer<? super T> consumer) {
        //if not present then will run ElseIf.elseIf()
        if (!isPresent())
            return Runnable::run; //<- functional method of @FunctionalInterface (ElseIf.elseIf)

        consumer.accept(value);
        return runnable -> {};
    }

    public NullableOptional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);

        if (!isPresent())
            return this;
        else
            return predicate.test(value) ? this : empty();
    }

    public<U> NullableOptional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);

        if (!isPresent())
            return empty();
        else
            return NullableOptional.ofNullable(mapper.apply(value));
    }

    public<U> NullableOptional<U> flatMap(Function<? super T, NullableOptional<U>> mapper) {
        Objects.requireNonNull(mapper);

        if (!isPresent())
            return empty();
        else
            return Objects.requireNonNull(mapper.apply(value));
    }

    @Override
    public String toString() {
        return isPresent() ? String.format("NullableOptional[%s]", get()) : "NullableOptional.empty";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof NullableOptional))
            return false;

        NullableOptional<?> other = (NullableOptional<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
