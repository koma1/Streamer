package pw.komarov.streamer;

import java.util.Comparator;
import java.util.stream.Stream;

public class StressRunner {
    public static void main(String[] args) {
        final Stream<?> stream =
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
                    .map(s -> s.equals("12") ? "twelve" : s.equals("18") ? "eighteen" : String.format("(%s)unknown", s)) //то, что знаем, преобразуем в строки
                ; //("(10)unknown", "twelve", "eighteen")

        stream.forEach(System.out::println);
    }
}