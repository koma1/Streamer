package pw.komarov.streamer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CCEBugFixTests {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void mapTest() {
        Streamer<String> streamer = Streamer.of("10", "5", "15");
        streamer.map(Integer::valueOf);

        assertThrows(IllegalStateException.class, () -> streamer.forEach(System.out::println));
    }
}