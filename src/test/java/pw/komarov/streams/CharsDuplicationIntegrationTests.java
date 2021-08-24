package pw.komarov.streams;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/*
    Напишите функцию, которая для строки возвращает первый символ, встречающийся в строке только один раз.
*/

class CharsDuplicationIntegrationTests {
    private static char getFirstUniqueChar(String string) {
        Objects.requireNonNull(string);

        if (string.isEmpty())
            throw new IllegalArgumentException("Empty string");

        char first = string.charAt(0);

        if (string.length() == 1)
            return first;

        if (string.replaceAll(String.valueOf(first), "").isEmpty())
            throw new IllegalArgumentException("All characters are identical");

        return (char) Streamer.from(string)
                .reversed()
                .distinct()
                .findLast()
                    .orElseThrow(() -> new RuntimeException("unreachable statement"))
                        .intValue();
    }

    @Test
    void mainTest() {
        //noinspection SpellCheckingInspection
        assertEquals('c', getFirstUniqueChar("abacaba"));
        assertEquals('m', getFirstUniqueChar("melon"));
        assertEquals('x', getFirstUniqueChar("extends"));
    }

    @Test
    void exceptionsTest() {
        assertThrows(NullPointerException.class, () -> getFirstUniqueChar(null));
        assertThrows(IllegalArgumentException.class, () -> getFirstUniqueChar(""));
        assertThrows(IllegalArgumentException.class, () -> getFirstUniqueChar("aaa"));
    }
}
