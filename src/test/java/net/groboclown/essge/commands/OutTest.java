package net.groboclown.essge.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class OutTest {
    @Test
    void wrapText() {
        assertEquals(
                Out.wrapText("", 5),
                List.of()
        );
        assertEquals(
                List.of("a b"),
                Out.wrapText("a b", 5)
        );
        assertEquals(
                List.of("a b c"),
                Out.wrapText("a b c", 5)
        );
        assertEquals(
                List.of("a b c", "d"),
                Out.wrapText("a b c d", 5)
        );
        assertEquals(
                List.of("12345", "6"),
                Out.wrapText("123456", 5)
        );
        assertEquals(
                List.of("a b", "12345"),
                Out.wrapText("a b 12345", 5)
        );
        assertEquals(
                List.of("a b", "12345", "678"),
                Out.wrapText("a b 12345678", 5)
        );
        assertEquals(
                List.of("a b", "12345", "678 c", "d"),
                Out.wrapText("a b 12345678 c d", 5)
        );
        assertEquals(
                List.of("a", "b"),
                Out.wrapText("a\nb", 5)
        );
        assertEquals(
                List.of("  a", "b"),
                Out.wrapText("  a\nb", 5)
        );
        assertEquals(
                List.of("  a", "  b"),
                Out.wrapText("  a\n  b", 5)
        );
    }
}
