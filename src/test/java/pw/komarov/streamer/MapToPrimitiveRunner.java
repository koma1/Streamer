package pw.komarov.streamer;

import java.util.concurrent.ThreadLocalRandom;

public class MapToPrimitiveRunner {
    public static void main(String[] args) {
        System.out.println("Case #1 (sequential random ints)");
        Streamer
                .generate(() -> {
                        try { Thread.sleep(500); } catch (Exception ignored) {}
                        return String.valueOf(ThreadLocalRandom.current().nextInt());
                })
                    .limit(5)
                    .mapToInt(Integer::valueOf)
                .forEach(System.out::println);

        System.out.println("\nCase #2 (sequential random longs)");
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextLong());
                })
                .limit(5)
                .mapToLong(Long::valueOf)
                .forEach(System.out::println);

        System.out.println("\nCase #3 (sequential random doubles)");
        Streamer
                .generate(() -> {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    return String.valueOf(ThreadLocalRandom.current().nextDouble());
                })
                .limit(5)
                .mapToDouble(Double::valueOf)
                .forEach(System.out::println);
    }
}
