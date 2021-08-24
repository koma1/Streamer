package pw.komarov.streams;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AdditionalFeaturesTests {
    @Test
    void reversedTest() {
        assertArrayEquals(new Integer[]{2,5,1}, Streamer.of(1, 5, 2).reversed().toArray());
    }

    @Test
    void findLastTest() throws Exception {
        assertEquals(2, Streamer.of(1, 5, 2).findLast().orElseThrow(() -> new Exception("")));
    }
}
