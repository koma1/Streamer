package pw.komarov.streamer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class StreamerInstancesCreationExamples {
    public static void main(String[] args) {
        Streamer.empty(); //empty streamer

        Streamer.of(new Object()); //from single Object
        Streamer.of(new Integer[]{1, 4, 8, 17}); //from an Array
        Streamer.of(Arrays.asList(7.34, 9, 18.7, 3)); //from Iterable (List in this case)
        Streamer.of("Foo", "Bar", "Juice", "hello", "streamer"); //from values

        //Infinite
        Streamer.generate(() -> ThreadLocalRandom.current().nextInt()); //infinite streamer (with random integer)
        Streamer.generate(() ->
            {
                List strings = Arrays.asList("randomly", "returned", "string", "value");
                return strings.get(ThreadLocalRandom.current().nextInt(strings.size()));
            }); //infinite streamer (with random string from an List<>)

        Streamer.iterate(100, (i) -> i * 2); //infinite streamer (started from 100 and multiple on two, over the each iteration step{100,200,400.........n})
    }
}
