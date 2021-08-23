package pw.komarov.streams;

import java.util.Comparator;

public class SocialsGeneratorsRunner {
    private static void armyGenerator() {
        //Армейский призыв
        System.out.println("Армейский призыв:");
        Streamer
                .generate(Person.Utils::generateRandomPerson) //сгенерируем персону
                .filter(person -> person.gender == Person.Gender.MALE) //отберем по полу
                .filter(person -> person.age >= 18 && person.age <= 27) //отберем по возрасту
                .limit(10) //остановим генерацию, когда набрали 10 подходящих "кандидатов"
                .sorted(Comparator.comparingInt(Person::getAge).thenComparing(Person::getName)) //отсортируем сначала по возрасту, затем по ФИО
            .forEach(System.out::println);
    }

    private static void oldPeoplesGenerator() {
        //Пенсионеры
        System.out.println("----------------\nПенсионеры:");
        System.out.println();
        Streamer
                .generate(Person.Utils::generateRandomPerson)
                .filter(person ->
                        (person.gender == Person.Gender.MALE && person.age >= 63)
                                ||
                                (person.gender == Person.Gender.FEMALE && person.age >= 58))
                .limit(10)
                .sorted(Comparator.comparing(Person::getGender).thenComparingInt(Person::getAge).reversed())
            .forEach(System.out::println);
    }

    public static void main(String[] args) {
        armyGenerator();
        oldPeoplesGenerator();
    }
}
