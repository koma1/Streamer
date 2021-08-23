package pw.komarov.streams;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class Person {
    private final static AtomicInteger idGenerator = new AtomicInteger();

    enum Gender {MALE, FEMALE}

    final int id = idGenerator.incrementAndGet();

    final String name;
    final Gender gender;
    final int age;

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }

    public int getId() {
        return id;
    }

    final Date createdAt = new Date();

    Person(String name, Gender gender, int age) {
        this.name = name;
        this.gender = gender;
        this.age = age;
    }

    @Override
    public String toString() {
        return String.format("\"%s\", %d (%s)", name, age, gender == Gender.MALE ? 'M' : 'F');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        Person person = (Person) o;

        if (age != person.age) return false;
        if (!name.equalsIgnoreCase(person.name)) return false;
        return gender == person.gender;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + gender.hashCode();
        result = 31 * result + age;
        return result;
    }

    public static class Utils {
        public static Person generateRandomPerson() {
            boolean isMale = ThreadLocalRandom.current().nextBoolean();

            return new Person(
                    isMale
                            ? getRandomElement(firstNamesMale) + " " + getRandomElement(lastNamesMale)
                            : getRandomElement(firstNamesFemale) + " " + getRandomElement(lastNamesFemale),
                    isMale
                            ? Person.Gender.MALE
                            : Person.Gender.FEMALE,
                    ThreadLocalRandom.current().nextInt(60) + 20
            );
        }

        public static List<Person> generatePersons(int count) {
            List<Person> persons = new ArrayList<>();
            for (int i = 1; i <= count; i++)
                persons.add(generateRandomPerson());

            return persons;
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
}
