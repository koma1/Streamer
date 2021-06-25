package pw.komarov.streamer;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

class Person {
    private final static AtomicInteger idGenerator = new AtomicInteger();

    enum Gender {MALE, FEMALE}

    final int id = idGenerator.incrementAndGet();

    final String name;
    final Gender gender;
    final int age;

    final Date createdAt = new Date();

    Person(String name, Gender gender, int age) {
        this.name = name;
        this.gender = gender;
        this.age = age;
    }
}

class PersonUtils {
    static Person generateRandomPerson() {
        boolean isMale = ThreadLocalRandom.current().nextBoolean();

        return new Person(
                isMale
                        ? getRandomElement(firstNamesMale) + " " + getRandomElement(lastNamesMale)
                        : getRandomElement(firstNamesFemale) + " " + getRandomElement(lastNamesFemale),
                isMale
                        ? Person.Gender.MALE
                        : Person.Gender.FEMALE,
                ThreadLocalRandom.current().nextInt(10) + 25
        );
    }

    private static <T> T getRandomElement(T[] ts) {
        return ts[ThreadLocalRandom.current().nextInt(ts.length)];
    }

    //Ru
    private static final String[] firstNamesFemaleRuRu = {"Василиса", "Злата", "Алёна", "Александра", "Софья", "Арина", "Агата", "Алия", "Анна", "Ева"};
    private static final String[] firstNamesMaleRuRu = {"Марк", "Евгений", "Вячеслав", "Михаил", "Артём", "Алексей", "Александр", "Антон", "Павел", "Адам"};
    private static final String[] lastNamesMaleRuRu = {"Митрофанов", "Васильев", "Пышов", "Гремячин", "Лукоянов", "Бабин", "Александров", "Максимов", "Братков", "Пономарев"};
    private static final String[] lastNamesFemaleRuRu = {"Иванникова", "Малышева", "Хлынева", "Золотова", "Машкина", "Овчинникова", "Грибоедова", "Колыванова", "Делягина", "Мизурина"};

    private static final String[] firstNamesMale    = firstNamesMaleRuRu;
    private static final String[] lastNamesMale     = lastNamesMaleRuRu;
    private static final String[] firstNamesFemale  = firstNamesFemaleRuRu;
    private static final String[] lastNamesFemale   = lastNamesFemaleRuRu;
}