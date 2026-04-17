package net.groboclown.essge.commands;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Generate output text.
 * <p>
 * It'd be nice to allow screen wrapping, but, in simple Java, that's
 * really, really hard to get the console width.
 */
public class Out {
    public static Out INSTANCE = new Out();
    public static Out ERR = new Out("", System.err);

    private final PrintStream out;
    private final String firstIndent;
    private final String nextIndent;
    private boolean onStart = true;

    public Out(String firstIndent, String nextIndent, PrintStream out) {
        this.firstIndent = firstIndent;
        this.nextIndent = nextIndent;
        this.out = out;
    }

    public Out(String indent, PrintStream out) {
        this(indent, indent, out);
    }

    public Out() {
        this("", System.out);
    }

    /**
     * Print & Newline.
     */
    public static void n(String text) {
        INSTANCE.pLn(text);
    }

    /**
     * Print second+ line & Newline
     */
    public static void sn(String text) {
        INSTANCE.psLn(text);
    }

    /**
     * Partial line print (no newline)
     */
    public static void p(String text) {
        INSTANCE.pp(text);
    }

    public static void sp(String text) {
        INSTANCE.psp(text);
    }

    /**
     * Print & Newline.
     */
    public void pLn(String text) {
        this.indentPrintLine(this.firstIndent, text);
    }

    /**
     * Second+ Print & Newline.
     */
    public void psLn(String text) {
        this.indentPrintLine(this.nextIndent, text);
    }

    /**
     * Partial line print
     */
    public void pp(String text) {
        indentPartialPrint(this.firstIndent, text);
    }

    /**
     * Second+ partial line print
     */
    public void psp(String text) {
        indentPartialPrint(this.nextIndent, text);
    }

    /**
     * Split the text into at most 'len' characters.
     *
     * @param text text to split
     * @param width maximum split length
     * @return the split text
     */
    public static List<String> wrapText(String text, int width) {
        final List<String> ret = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        final StringBuilder remaining = new StringBuilder();
        int lineLen = 0;
        int remainingLen = 0;
        int remainingStart = 0;
        boolean onWord = false;
        boolean onNewline = true;
        for (char c: text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (lineLen == 0 && remainingLen == 0) {
                    if (onNewline) {
                        // Need to keep this whitespace for possible first line indent.
                        line.append(c);
                        lineLen++;
                        continue;
                    }
                    // else ignore initial indents.
                    continue;
                }
                if (onWord) {
                    // In the middle of reading a word, so make a split.
                    if (lineLen + remainingLen > width) {
                        // End Of Line.
                        appendLine(ret, line, remaining, remainingStart, width);
                        lineLen = line.length();
                        remainingLen = remaining.length();
                        remainingStart = 0;
                    } else {
                        line.append(remaining);
                        lineLen += remaining.length();
                        remaining.setLength(0);
                        remainingLen = 0;
                        remainingStart = 0;
                    }
                    onWord = false;
                }
                if (c == '\n') {
                    // Force an append line of the remaining.  Do not insert the newline.
                    if (!onNewline && !line.isEmpty()) {
                        appendLine(ret, line, remaining, remainingStart, width);
                    }
                    if (!line.isEmpty()) {
                        // There's still some remaining.
                        appendLine(ret, line, remaining, remainingStart, width);
                    }
                    lineLen = line.length();
                    remainingLen = remaining.length();
                    remainingStart = 0;
                    onNewline = true;
                    continue;
                }
                remaining.append(c);
                remainingLen++;
                remainingStart++;
                onNewline = false;
                continue;
            }
            remaining.append(c);
            remainingLen++;
            onNewline = false;
            onWord = true;
        }
        if (lineLen + remainingLen > width) {
            appendLine(ret, line, remaining, remainingStart, width);
            if (!line.isEmpty()) {
                ret.add(line.toString());
            }
            if (!remaining.isEmpty()) {
                ret.add(remaining.toString());
            }
        } else if (lineLen + remainingLen > 0) {
            line.append(remaining);
            ret.add(line.toString());
        }
        return ret;
    }

    // put the line into the ret, and add the remaining into the line
    private static void appendLine(
            final List<String> ret,
            final StringBuilder line,
            final StringBuilder remaining,
            final int remainingStart,
            final int width) {
        if (!line.isEmpty()) {
            ret.add(line.toString());
        }
        line.setLength(0);
        remaining.delete(0, remainingStart);
        int rLen = remaining.length();
        while (rLen > width) {
            ret.add(remaining.substring(0, width));
            remaining.delete(0, width);
            rLen -= width;
        }
        line.append(remaining);
        remaining.setLength(0);
    }

    /**
     * Print + Newline.
     */
    private void indentPrintLine(String initial, String text) {
        int start = 0;
        int len = text.length();
        String indent = initial;
        if (!this.onStart) {
            indent = "";
        }
        while (start < len) {
            int pos = text.indexOf('\n', start);
            if (pos < 0) {
                pos = len;
            }
            this.out.println(indent + text.substring(start, pos).stripTrailing());
            start = pos + 1;
            indent = this.nextIndent;
        }
        this.out.flush();
        this.onStart = true;
    }

    private void indentPartialPrint(String initial, String text) {
        int start = 0;
        int len = text.length();
        String indent = initial;
        boolean firstLine = false;
        if (!this.onStart) {
            indent = "";
            firstLine = true;
        }
        while (start < len) {
            int pos = text.indexOf('\n', start);
            if (pos < 0) {
                pos = len;
            }
            String part = text.substring(start, pos);
            this.out.print(indent + part);
            start = pos + 1;
            indent = "\n" + this.nextIndent;

            // The indent was printed.
            this.onStart = firstLine;
            firstLine = false;
        }
        this.out.flush();
    }


    public Out sub(String indent) {
        // Yes, next indent for both.
        return new Out(this.nextIndent + indent, this.out);
    }

    public Out sub(String first, String next) {
        // Yes, next indent for both.
        return new Out(this.nextIndent + first, this.nextIndent + next, this.out);
    }

    public void yaml(Object o) {
        yaml(o, Function.identity());
    }

    public void yaml(final Object src, Function<Object, Object> tForm) {
        final Object o = tForm.apply(src);
        if (o == null) {
            pLn("null");
            return;
        }
        if (o instanceof Boolean || o instanceof Number || o instanceof String || o.getClass().isEnum()) {
            pLn(o.toString());
            return;
        }
        if (o.getClass().isArray()) {
            pLn("");
            final Out next = sub("- ", "  ");
            for (Object item : (Object[]) o) {
                next.yaml(item, tForm);
            }
            return;
        }
        if (o instanceof Map<?, ?> m) {
            pLn("");
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                // Assume a simple entry type.
                psp(entry.getKey().toString() + ": ");
                final Out next = sub("  ", "  ");
                next.onStart = false;
                next.yaml(entry.getValue(), tForm);
            }
        }
    }
}
