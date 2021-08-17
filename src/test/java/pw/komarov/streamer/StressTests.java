package pw.komarov.streamer;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StressTests {
    @Test
    void test1() {
        final Streamer<?> streamer =
                Streamer
                        .of(108, 5, 12, 11, 4, 9, 7, 5)
                        .distinct() //(108, 5, 12, 11, 4, 9, 7, [5])
                        .skip(1)    //([108], 5, 12, 11, 4, 9, 7)
                        .limit(6)   //(5, 12, 11, 4, 9, 7)
                        .limit(5)   //(5, 12, 11, 4, 9, [7])
                        .sorted(Comparator.reverseOrder()) //(12, 11, 9, 5, 4)
                        .map(i -> i == 11 ? 12 : i) //(12, [11]->12, 9, 5, 4)
                        .distinct() //(12, [12], 9, 5, 4)
                        .map(i -> (i & 1) == 1 ? i * 2 : i) //(12, [9]->18, [5]->10, 4)
                        .sorted() //(4, 10, 12, 18)
                        .skip(1) //([4], 10, 12, 18)
                        .map(String::valueOf) //("10", "12", "18")
                        .map(s ->
                                s.equals("12")
                                ? "twelve"
                                    : s.equals("18")
                                    ? "eighteen"
                                        : String.format("(%s)unknown", s))
                ;

        assertArrayEquals(new String[]{"(10)unknown","twelve","eighteen"}, streamer.toArray());
    }

    @Test
    void fibonacciNumbersTest() {
        int[] actual = Streamer
                .iterate(new int[]{0,1}, ints -> new int[]{ints[1],ints[0] + ints[1]})
                    .limit(10)
                    .mapToInt(ints -> ints[1])
                .toArray();

        assertArrayEquals(new int[]{1,1,2,3,5,8,13,21,34,55}, actual);
    }

    @Test
    void emptyTest() {
        assertEquals(0, Streamer.empty().count());
    }

    @Test
    void fromMapTest() {
        Map<Integer,Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);

        assertArrayEquals(new Integer[]{1, 2, 3}, Streamer.from(map).map(Map.Entry::getKey).toArray());
        assertArrayEquals(new Integer[]{1, 2, 3}, Streamer.fromMapKeys(map).toArray());
        assertArrayEquals(new Integer[]{1, 2, 3}, Streamer.fromMapValues(map).toArray());
    }

    @Test
    void groupByTest() {
        Map<Boolean, Collection<Integer>> grouped = Streamer.of(1, 2, 3, 4).groupBy(integer -> integer % 2 == 0);

        assertArrayEquals(new Integer[]{1, 3}, grouped.get(false).toArray());
        assertArrayEquals(new Integer[]{2, 4}, grouped.get(true).toArray());
    }
}