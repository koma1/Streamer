package pw.komarov.streamer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnCloseTests {
    @Test
    void notTerminatedOnCloseRequestTest() {
        notTerminatedOnCloseRequestTest(Stream.generate(() -> 0));
        notTerminatedOnCloseRequestTest(Streamer.generate(() -> 0));
    }

    @SuppressWarnings({"deprecation", "ResultOfMethodCallIgnored"})
    private void notTerminatedOnCloseRequestTest(Stream<?> stream) {
        final AtomicBoolean closed = new AtomicBoolean();
        stream.onClose(() -> closed.set(true));

        final AtomicInteger counter = new AtomicInteger();
        final Thread thread = new Thread(() ->
                stream.forEach(o -> {
                    try { Thread.sleep(100); } catch (Exception ignored) {}
                    counter.incrementAndGet();
                })
        );
        thread.start();

        while (counter.get() < 5)
            try { Thread.sleep(10); } catch (Exception ignored) {}

        assertFalse(closed.get()); //ensure, that is false...
        stream.close();

        while (counter.get() < 10) //it closed before, but if we not terminate the thread, it will infinite
            try { Thread.sleep(10); } catch (Exception ignored) {}

        assertTrue(closed.get()); //changed because onClose() called forced
        thread.stop(); //<- terminate thread manually, stream doesn't stop
        assertTrue(counter.get() >= 10);
    }

    @Test
    void notCompletedOnCloseSequences() {
        notCompletedOnCloseSequences(Stream.of(1, 2, 3, 4, 5));
        notCompletedOnCloseSequences(Streamer.of(1, 2, 3, 4, 5));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void notCompletedOnCloseSequences(Stream<?> stream) {
        AtomicBoolean closed = new AtomicBoolean();

        assertFalse(closed.get()); //ensure, that is in false state

        stream = stream.onClose(() -> closed.set(true));
        stream.toArray();

        assertFalse(closed.get()); //after work is done, onClose() not completed
    }
}
