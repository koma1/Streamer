package pw.komarov.streamer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FlatMapRunner {
    public static void main(String[] args) {
        System.out.println("Test case #1 (sequential FlatMap)");
            Streamer
                    .generate(() -> {
                        new Sleep(500);
                        return Person.Utils.generateRandomPerson(); //generated person
                    }).limit(10) //ten persons is enough
            .flatMap(person -> Streamer.of(
                    "---------------------",
                    "ID: " + person.id,
                    "Name: " + person.name,
                    "Gender: " + person.gender,
                    "Age: " + person.age
            )).forEach(System.out::println);

        final AtomicInteger ai = new AtomicInteger();

        System.out.println("\nTest case #2 (empty odd result)");
        Streamer
                .generate(() -> {
                    new Sleep(500);
                    return Person.Utils.generateRandomPerson(); //generated person
                }).limit(10) //ten persons is enough
                .flatMap(person -> {
                    if ((ai.incrementAndGet() & 1) == 0)
                        return Streamer.empty();
                    else
                        return Streamer.of(
                                "---------------------",
                                "ID: " + person.id,
                                "Name: " + person.name,
                                "Gender: " + person.gender,
                                "Age: " + person.age);
                }).forEach(System.out::println);

        System.out.println("\nTest case #3 (FlatMap from empty parent stream)");
        Streamer
                .empty()
                .flatMap(o -> Stream.of(ai.get(), ai.get() * -1))
                .forEach(System.out::println);
    }

    private static class Sleep {
        private Sleep(long millis) {
            try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
        }
    }
}