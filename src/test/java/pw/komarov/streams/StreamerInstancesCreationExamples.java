package pw.komarov.streams;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class StreamerInstancesCreationExamples {

    @SuppressWarnings({"ResultOfMethodCallIgnored", "RedundantArrayCreation"})
    public static void main(String[] args) {
        printStreamMethod("empty", Streamer.empty()); //empty streams
        printStreamMethod("of[Object]", Streamer.of(new Object())); //from single Object
        printStreamMethod("of[ArrayOfObjects]", Streamer.of(new Integer[]{1, 4, 8, 17})); //from an Array of objects
        printStreamMethod("from[List]", Streamer.from(Arrays.asList(7.34, 9, 18.7, 3))); //from Iterable (List, at this case)
        printStreamMethod("of[Values]", Streamer.of("Foo", "Bar", "Juice", "hello", "streams")); //from values

        //Infinites
        Streamer.generate(() -> ThreadLocalRandom.current().nextInt()); //infinite streams (with random integer)
        Streamer.generate(() ->
            {
                List strings = Arrays.asList("randomly", "returned", "string", "value");
                return strings.get(ThreadLocalRandom.current().nextInt(strings.size()));
            }); //infinite streams (with random string from an List<>)

        Streamer.iterate(100, (i) -> i * 2); //infinite streams (started from 100 and multiple on two, over the each iteration step{100,200,400.........n})
    }

    private static void printStreamMethod(String method, Stream<?> stream) {
        System.out.print(method + "(): ");
        stream.forEach((v) -> System.out.print(v + " "));

        System.out.println();
    }
}
