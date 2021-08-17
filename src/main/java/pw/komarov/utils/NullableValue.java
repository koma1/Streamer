package pw.komarov.utils;

public class NullableValue<E> {
    private static final NullableValue<?> NULLABLE = new NullableValue<>(null);

    private final E value;

    private NullableValue(E value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <E> NullableValue<E> of(E value) {
        return value == null ? (NullableValue<E>) NULLABLE : new NullableValue<>(value);
    }

    public E get() {
        return value;
    }
}
