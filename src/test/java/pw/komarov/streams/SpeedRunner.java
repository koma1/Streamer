package pw.komarov.streams;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SpeedRunner {
    public static void main(String[] args) {
        //CASE #1 - IntStream.iterate
        System.out.println("-----------------\nCASE #1 - IntStream.iterate");
        printStreamTiming(IntStream.iterate(1, i -> i + 1));

        //CASE #2 - Stream.iterate
        System.out.println("-----------------\nCASE #2 - Stream.iterate");
        printStreamTiming(Stream.iterate(1, i -> i + 1));

        //CASE #3 - Streamer.iterate
        System.out.println("-----------------\nCASE #3 - Streamer.iterate");
        printStreamTiming(Streamer.iterate(1, i -> i + 1));


        System.out.println("-----------------\nCASE #4 - for-loop 1->100000010");
        long start = System.currentTimeMillis();
        for (int i = 1; i <= 100000010; i++)
            if (i > 100000000)
                System.out.print("");
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Elapsed time: %dms (%s)\n", elapsed, new SimpleDateFormat("mm:ss.S").format(new Date(elapsed)));

        System.out.println("-----------------\nCASE #5 - for-loop (infinite iterator) 1->100000010");
        start = System.currentTimeMillis();

        int i = 0;
        for (Iterator<Integer> itr = new Streamer.InfiniteIterator<>(1, n -> n + 1); i <= 100000010;i = itr.next())
            if (i > 100000000)
                System.out.print("");

        elapsed = System.currentTimeMillis() - start;
        System.out.printf("Elapsed time: %dms (%s)\n", elapsed, new SimpleDateFormat("mm:ss.S").format(new Date(elapsed)));
    }

    private static void printStreamTiming(Stream<Integer> stream) {
        long start = System.currentTimeMillis();

        stream
                .filter(i -> i > 100000000)
                .limit(10)
                .forEach(i -> {});

        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("Elapsed time: %dms (%s)\n", elapsed, new SimpleDateFormat("mm:ss.S").format(new Date(elapsed)));
    }

    private static void printStreamTiming(IntStream stream) {
        long start = System.currentTimeMillis();

        stream
                .filter(i -> i > 100000000)
                .limit(10)
                .forEach(i -> {});

        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("Elapsed time: %dms (%s)\n", elapsed, new SimpleDateFormat("mm:ss.S").format(new Date(elapsed)));
    }
}
