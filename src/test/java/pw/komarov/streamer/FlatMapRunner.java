package pw.komarov.streamer;

import java.util.stream.Stream;

public class FlatMapRunner {
    public static void main(String[] args) {
        final Stream<Person> personsStream =
                Streamer
                        .generate(() -> {
                            new Sleep(500);
                            return PersonUtils.generateRandomPerson(); //generated person
                        }).limit(10) //ten persons is enough
                ;

        final Stream<String> personDetailsStream =
                personsStream.flatMap(person -> Streamer.of(
                        "---------------------",
                        "ID: " + person.id,
                        "Name: " + person.name,
                        "Gender: " + person.gender,
                        "Age: " + person.age
                ))
                ;

        personDetailsStream.forEach(System.out::println);
    }

    private static class Sleep {
        private Sleep(long millis) {
            try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
        }
    }
}