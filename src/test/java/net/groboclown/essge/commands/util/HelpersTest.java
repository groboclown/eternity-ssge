package net.groboclown.essge.commands.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class HelpersTest {
    @Test
    void readArgFromReader_actions1() throws IOException {
        List<String> res = Helpers.readArgFromReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("actions1.txt"))));
        assertEquals(115, res.size());
    }

    @Test
    void readArgFromReader() throws IOException {
        List<String> res = Helpers.readArgFromReader(new StringReader("a b c\n123   456\n2\n/ a"));
        assertEquals(
                List.of(
                        "a", "b", "c", "123", "456", "2", "/", "a"
                ),
                res
        );
    }
}
