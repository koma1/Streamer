package pw.komarov.streamer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelayedIterableStartTest {
    private List<Integer> integers;

    @BeforeEach
    void beforeEach() {
        integers = new ArrayList<>(Arrays.asList(1, 5, 18, 2));
    }

    @Test
    void delayedStartTest1() {
        Streamer<Integer> streamer = Streamer.from(integers);

        integers.add(ThreadLocalRandom.current().nextInt());

        assertArrayEquals(integers.toArray(), streamer.toArray());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void delayedStartTest2() {
        Streamer<Integer> streamer = Streamer.from(integers.iterator());

        integers.add(ThreadLocalRandom.current().nextInt());

        assertThrows(ConcurrentModificationException.class, streamer::toArray);
    }
}
