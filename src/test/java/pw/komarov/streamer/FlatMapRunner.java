package pw.komarov.streamer;

import java.util.stream.Stream;

public class FlatMapRunner {
    public static void main(String[] args) {
        final Stream<Person> personsStream =
                Streamer
                        .generate(() -> {
                                final Person person = PersonUtils.generateRandomPerson();
                                if ((person.id & 1) == 0) //odd id?...
                                    try { Thread.sleep(1000); } catch (Exception ignored) {} //... if id is odd then sleep

                                return person; //generated person
                            })
                        .limit(10) //ten persons is enough
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
}