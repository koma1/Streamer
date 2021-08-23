package pw.komarov.streams;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class FlatMapToPrimitiveRunner {
    private static final AtomicInteger ai = new AtomicInteger();

    public static void main(String[] args) {
        ///// Ints
        System.out.println("Test case #1(int) (sequential FlatMap)");
        ai.set(0);
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextInt());
                })
                .peek(s -> System.out.println("s = " + s))
                .limit(5)
                .flatMapToInt(s -> (IntStream.of(Integer.valueOf(s), Integer.valueOf(s) * -1)))
                .forEach(System.out::println);

        System.out.println("\nTest case #2(int) (empty odd result)");
        ai.set(0);
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextInt());
                })
                .peek(s -> System.out.println("s = " + s))
                .limit(5)
                .flatMapToInt(s -> (
                        (ai.incrementAndGet() & 1) == 0
                                ? IntStream.empty()
                                : IntStream.of(Integer.valueOf(s), Integer.valueOf(s) * -1)
                ))
                .forEach(System.out::println);

        System.out.println("\nTest case #3(int) (FlatMap from empty parent stream)");
        ai.set(0);
        Streamer
                .empty()
                .flatMapToInt(o -> (IntStream.of(ai.get(), ai.get() * -1)))
                .forEach(System.out::println);

        ///// Longs
        System.out.println("\n\n");
        System.out.println("Test case #1(long) (sequential FlatMap)");
        ai.set(0);
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextLong());
                })
                .peek(s -> System.out.println("s = " + s))
                .limit(5)
                .flatMapToLong(s -> (LongStream.of(Long.valueOf(s), Long.valueOf(s) * -1)))
                .forEach(System.out::println);

        System.out.println("\nTest case #2(long) (empty odd result)");
        ai.set(0);
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextLong());
                })
                .peek(s -> System.out.println("s = " + s))
                .limit(5)
                .flatMapToLong(s -> (
                        (ai.incrementAndGet() & 1) == 0
                                ? LongStream.empty()
                                : LongStream.of(Long.valueOf(s), Long.valueOf(s) * -1)
                ))
                .forEach(System.out::println);

        System.out.println("\nTest case #3(long) (FlatMap from empty parent stream)");
        ai.set(0);
        Streamer
                .empty()
                .flatMapToLong(o -> (LongStream.of(ai.get(), ai.get() * -1)))
                .forEach(System.out::println);

        ///// Doubles
        System.out.println("\n\n");
        System.out.println("Test case #1(double) (sequential FlatMap)");
        ai.set(0);
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextDouble());
                })
                .peek(s -> System.out.println("s = " + s))
                .limit(5)
                .flatMapToDouble(s -> (DoubleStream.of(Double.valueOf(s), Double.valueOf(s) * -1)))
                .forEach(System.out::println);

        System.out.println("\nTest case #2(double) (empty odd result)");
        ai.set(0);
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextDouble());
                })
                .peek(s -> System.out.println("s = " + s))
                .limit(5)
                .flatMapToDouble(s -> (
                        (ai.incrementAndGet() & 1) == 0
                                ? DoubleStream.empty()
                                : DoubleStream.of(Double.valueOf(s), Double.valueOf(s) * -1)
                ))
                .forEach(System.out::println);

        System.out.println("\nTest case #3(double) (FlatMap from empty parent stream)");
        ai.set(0);
        Streamer
                .empty()
                .flatMapToDouble(o -> (DoubleStream.of(ai.get(), ai.get() * -1)))
                .forEach(System.out::println);
    }
}
