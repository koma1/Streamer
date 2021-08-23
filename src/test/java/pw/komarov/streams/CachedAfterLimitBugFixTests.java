package pw.komarov.streams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CachedAfterLimitBugFixTests {
    private final StringBuilder sb = new StringBuilder();
    private final AtomicInteger ai = new AtomicInteger();

    @BeforeEach
    void beforeEach() {
        sb.delete(0, sb.length());
        ai.set(0);
    }

    @Test
    void test1() {
        Streamer.generate(ai::getAndIncrement).limit(10).forEach((v) -> sb.append(v).append(" "));
        sb.append(ai.get());
        assertEquals("0 1 2 3 4 5 6 7 8 9 10", sb.toString());
    }

    @Test
    void test2() {
        Streamer.generate(ai::getAndIncrement).limit(10).distinct().filter(i -> i != 5).limit(8).limit(16).forEach((v) -> sb.append(v).append(" "));
        sb.append(ai.get());
        assertEquals("0 1 2 3 4 6 7 8 9", sb.toString());
    }
}
