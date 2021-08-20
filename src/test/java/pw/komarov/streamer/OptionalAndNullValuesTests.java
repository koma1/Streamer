package pw.komarov.streamer;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
class OptionalAndNullValuesTests {

    @Test
    void findFirstTest() {
        assertThrows(NullPointerException.class, () -> Streamer.of(null, 1, 2, 3)
                .findFirst());
    }

    @Test
    void findAnyTest() {
        assertThrows(NullPointerException.class, () -> Streamer.of(null, 1, 2, 3)
                .findAny());
    }

    @Test
    void minTest() {
        assertThrows(NullPointerException.class, () -> Streamer.of(1, null, 2, 3)
                .min(Comparator.nullsFirst(Integer::compareTo)));
    }

    @Test
    void maxTest() {
        assertThrows(NullPointerException.class, () -> Streamer.of(1, null, 2, 3)
                .max(Comparator.nullsLast(Integer::compareTo)));
    }

    @SuppressWarnings({"WrapperTypeMayBePrimitive", "ConstantConditions"})
    @Test
    void reduceTest() {
        assertThrows(NullPointerException.class, () -> Streamer.of(1, null, 2, 3).reduce((total, curr) -> {
            Integer t = total + (curr == null ? 0 : curr);
            return (t == 6 ? null : t);
        }));
    }
}
