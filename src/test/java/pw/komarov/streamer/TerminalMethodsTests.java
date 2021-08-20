package pw.komarov.streamer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

class TerminalMethodsTests {
    private Streamer<Integer> streamer;

    @BeforeEach
    void beforeEach() {
        streamer = Streamer.iterate(1, i -> i + 1).limit(10);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void anyMatchTest() {
        assertTrue(streamer.anyMatch(integer -> integer == 10));
        beforeEach();
        assertFalse(streamer.anyMatch(integer -> integer < 1));

        assertThrows(IllegalStateException.class, () -> streamer.anyMatch(i -> true));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void allMatchTest() {
        assertTrue(streamer.allMatch(integer -> integer <= 10));
        beforeEach();
        assertFalse(streamer.allMatch(integer -> integer < 10));

        assertThrows(IllegalStateException.class, () -> streamer.allMatch(i -> true));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void noneMatchTest() {
        assertTrue(streamer.noneMatch(integer -> integer > 10));
        beforeEach();
        assertFalse(streamer.noneMatch(integer -> integer < 10));

        assertThrows(IllegalStateException.class, () -> streamer.noneMatch(i -> true));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void findAnyTest() {
        assertTrue(streamer.findAny().get() <= 10);

        assertThrows(IllegalStateException.class, () -> streamer.findAny());

        streamer = Streamer.of(null, 1, 2);
        assertThrows(NullPointerException.class, () -> streamer.findAny());

        streamer = Streamer.empty();
        assertFalse(streamer.findAny().isPresent());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void forEachTest() {
        List<Integer> integers = new ArrayList<>();
        streamer.forEach(integers::add);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, integers.toArray());

        assertThrows(IllegalStateException.class, () -> streamer.forEach(null));
    }

    @Test
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ResultOfMethodCallIgnored"}) //OptionalGetWithoutIsPresent - throws an exception if not present
    void minTest() {
        assertEquals(1, streamer.min(Integer::compareTo).get());
        assertThrows(IllegalStateException.class, () -> streamer.min(Integer::compareTo));
    }

    @Test
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ResultOfMethodCallIgnored"}) //let's throws an exception if optional is null
    void maxTest() {
        assertEquals(10, streamer.max(Integer::compareTo).get());
        assertThrows(IllegalStateException.class, () -> streamer.max(Integer::compareTo));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void countTest() {
        assertEquals(10, streamer.count());
        assertThrows(IllegalStateException.class, () -> streamer.count());
    }

    @SuppressWarnings({"FuseStreamOperations", "ResultOfMethodCallIgnored"})
    @Test
    void collectTest() {
        assertArrayEquals(new Integer[]{1,2,3,4,5,6,7,8,9,10}, streamer.collect(Collectors.toList()).toArray());
        assertThrows(IllegalStateException.class, () -> streamer.collect(Collectors.toList()));

        beforeEach();
        assertArrayEquals(new Integer[]{1,2,3,4,5,6,7,8,9,10}, streamer.collect(
                ArrayList::new,
                (ArrayList::add),
                null)
            .toArray());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void toArrayTest() {
        assertArrayEquals(new Integer[]{1,2,3,4,5,6,7,8,9,10}, streamer.toArray());
        assertThrows(IllegalStateException.class, () -> streamer.toArray());

        beforeEach();
        assertArrayEquals(new Integer[]{1,2,3,4,5,6,7,8,9,10}, streamer.toArray(Integer[]::new));

        beforeEach();
        assertArrayEquals(new Integer[]{1,2,3,4,5,6,7,8,9,10,null}, streamer.toArray((size) -> new Integer[size + 1]));

        beforeEach();
        assertThrows(IndexOutOfBoundsException.class, () -> streamer.toArray((size) -> new Integer[size - 1]));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void reduce1Test() {
        assertNull(Streamer.empty().reduce(null, (total, current) -> total));
        assertEquals(-49, Streamer.of(50, 5, 3, 1).reduce(10, (total, current) -> total - current));
        assertEquals(10, Streamer.empty().reduce(10, (total, current) -> total));
        assertThrows(NullPointerException.class, () -> Streamer.of(50, 5, 3, 1).reduce(null, (total, current) -> total - current));
        assertThrows(NullPointerException.class, () -> Streamer.empty().reduce(null, null));

        streamer.reduce(1, (total, current) -> total - current);
        assertThrows(IllegalStateException.class, () -> streamer.reduce(1, (total, current) -> total - current));
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "OptionalGetWithoutIsPresent"})
    @Test
    void reduce2Test() {
        assertEquals(40, Streamer.of(50, 5, 3, 2).reduce((total,current) -> total - current).get());
        assertFalse(Streamer.empty().reduce((total,current) -> total).isPresent());

        streamer.reduce((total, current) -> total - current);
        assertThrows(IllegalStateException.class, () -> streamer.reduce((total, current) -> total - current));
    }

    @Test
    void reduce3RandomsTest() {
        Map<Person, Integer> salaries = new HashMap<>();

        Streamer.generate(Person.Utils::generateRandomPerson).limit(10).forEach(
                person -> salaries.put(person, ThreadLocalRandom.current().nextInt(1000) * 100)
        );

        reduce3Test(salaries);
    }

    @Test
    void reduce3FixedTest() {
        Map<Person, Integer> salaries = new HashMap<>();

        salaries.put(new Person("Василиса Малышева", Person.Gender.FEMALE, 34), 14400);
        salaries.put(new Person("Алексей Пышов", Person.Gender.MALE, 29), 76000);
        salaries.put(new Person("Вячеслав Лукоянов", Person.Gender.MALE, 33), 66100);
        salaries.put(new Person("Михаил Максимов", Person.Gender.MALE, 27), 17700);
        salaries.put(new Person("Алексей Максимов", Person.Gender.MALE, 33), 55600);
        salaries.put(new Person("Евгений Братков", Person.Gender.MALE, 25), 74600);
        salaries.put(new Person("Вячеслав Лукоянов", Person.Gender.MALE, 27), 700);
        salaries.put(new Person("Марк Максимов", Person.Gender.MALE, 26), 62000);
        salaries.put(new Person("Михаил Митрофанов", Person.Gender.MALE, 30), 26000);
        salaries.put(new Person("Василиса Хлынева", Person.Gender.FEMALE, 26), 35700);

        reduce3Test(salaries);
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    void reduce3Test(Map<Person, Integer> salaries) {
        long amountForeach = Streamer.fromMapValues(salaries).filter(salary -> salary < 30000).mapToLong(Integer::longValue).sum();

        long amount;

        amount = salaries.entrySet().stream()
                .reduce(
                        0,
                        (total, entry) -> entry.getValue() < 30000 ? total + entry.getValue() : total,
                        (total, current) -> 0);
        assertEquals(amountForeach, amount);

        salaries.clear();

        amount = salaries.entrySet().stream()
                .reduce(
                        0,
                        (total, entry) -> entry.getValue() < 30000 ? total + entry.getValue() : total,
                        (total, current) -> total + current);
        assertEquals(0, amount);

        streamer.reduce(0, ((x,y) -> 0), (x,y) -> 0);
        assertThrows(IllegalStateException.class, () -> streamer.reduce(0, ((x,y) -> 0), (x,y) -> 0));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void iteratorTest() {
        Iterator<Integer> iterator = streamer.iterator();

        assertThrows(IllegalStateException.class, streamer::iterator);

        int sum = 0;
        while (iterator.hasNext())
            sum+=iterator.next();

        assertThrows(NoSuchElementException.class, iterator::next);

        assertEquals(55, sum);
    }
}
