package easyIO;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/** Tests of the line terminators accepted by a scanner. */
public class LineTerminatorTests {
    private static Scanner scanner(String input, LineTerminators t) {
        Scanner s = new Scanner(new StringReader(input), "in");
        s.setLineTerminators(t);
        return s;
    }

    /** The line number of each character of the input, as a string of digits. */
    private static String lineNumbers(String input, LineTerminators t) throws EOF {
        Scanner s = scanner(input, t);
        StringBuilder b = new StringBuilder();
        while (s.hasNext()) {
            b.append(s.location().lineNo());
            s.nextCodePoint();
        }
        return b.toString();
    }

    @Test void defaultIsLfCrlf() {
        assertEquals(LineTerminators.LF_CRLF, new Scanner(new StringReader(""), "in").lineTerminators());
    }

    @Test void lineNumbers() throws EOF {
        String input = "a\nb\r\nc\rd";
        assertEquals("12223333", lineNumbers(input, LineTerminators.LF));
        assertEquals("12223333", lineNumbers(input, LineTerminators.LF_CRLF));
        assertEquals("12233344", lineNumbers(input, LineTerminators.LF_CRLF_CR));
    }

    @Test void columnsRestartAfterCr() throws EOF {
        Scanner s = scanner("ab\rcd", LineTerminators.LF_CRLF_CR);
        for (int i = 0; i < 4; i++) s.nextCodePoint();
        assertEquals(2, s.location().lineNo());
        assertEquals(2, s.location().column());
    }

    @Test void blankLinesWithCr() throws EOF {
        assertEquals("12334", lineNumbers("a\r\rb\r", LineTerminators.LF_CRLF_CR));
        assertEquals("2333", lineNumbers("\r\r\nb", LineTerminators.LF_CRLF_CR));
    }

    @Test void newlineAccepts() throws Exception {
        assertNewline("\n", LineTerminators.LF, true, "");
        assertNewline("\r\n", LineTerminators.LF, false, "\r\n");
        assertNewline("\r\nx", LineTerminators.LF_CRLF, true, "x");
        assertNewline("\rx", LineTerminators.LF_CRLF, false, "\rx");
        assertNewline("\rx", LineTerminators.LF_CRLF_CR, true, "x");
        assertNewline("\r\nx", LineTerminators.LF_CRLF_CR, true, "x");
        assertNewline("\r\rx", LineTerminators.LF_CRLF_CR, true, "\rx");
        assertNewline("x", LineTerminators.LF_CRLF_CR, false, "x");
    }

    private static void assertNewline(String input, LineTerminators t, boolean accepted, String rest)
            throws EOF {
        Scanner s = scanner(input, t);
        try {
            s.newline();
            assertTrue(accepted, "newline() accepted " + input);
        } catch (UnexpectedInput e) {
            assertFalse(accepted, "newline() rejected " + input);
        }
        StringBuilder b = new StringBuilder();
        while (s.hasNext()) b.appendCodePoint(s.nextCodePoint());
        assertEquals(rest, b.toString());
    }

    @Test void nextLineIncludesTerminator() throws UnexpectedInput {
        Scanner s = scanner("a\rb\r\nc\nd", LineTerminators.LF_CRLF_CR);
        assertEquals("a\r", s.nextLine());
        assertEquals("b\r\n", s.nextLine());
        assertEquals("c\n", s.nextLine());

        s = scanner("a\rb\r\nc", LineTerminators.LF_CRLF);
        assertEquals("a\rb\r\n", s.nextLine());
    }

    @Test void eolWithCr() throws EOF {
        Scanner s = scanner("a b\r\ncd", LineTerminators.LF_CRLF_CR);
        s.eol();
        assertEquals('c', s.nextCodePoint());

        s = scanner("a b\rcd", LineTerminators.LF_CRLF_CR);
        s.eol();
        assertEquals('c', s.nextCodePoint());
    }

    @Test void trailingWhitespaceStopsAtCr() throws EOF {
        Scanner s = scanner("  \rx", LineTerminators.LF_CRLF_CR);
        s.trailingWhitespace();
        assertEquals('\r', s.peek());

        s = scanner("  \rx", LineTerminators.LF_CRLF);
        s.trailingWhitespace();
        assertEquals('x', s.peek());
    }
}
