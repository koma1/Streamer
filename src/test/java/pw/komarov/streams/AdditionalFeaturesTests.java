package pw.komarov.streams;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AdditionalFeaturesTests {
    @Test
    void reversedTest() {
        assertArrayEquals(new Integer[]{2, 5, 1}, Streamer.of(1, 5, 2).reversed().toArray());
    }

    @Test
    void findLastTest() throws Exception {
        assertEquals(2, Streamer.of(1, 5, 2).findLast().orElseThrow(() -> new Exception("")));
    }

    @Test
    void lastTest() {
        assertArrayEquals(new Integer[]{4, 12}, Streamer.of(1, 5, 2, 4, 12).last(2).toArray());
        assertArrayEquals(new Integer[]{1, 5}, Streamer.of(1, 5).last(2).toArray());
        assertArrayEquals(new Integer[]{1, 5, 3, 7}, Streamer.of(1, 5, 3, 7).last(5).toArray());
        assertArrayEquals(new Integer[]{}, Streamer.empty().last(5).toArray());
    }
}
