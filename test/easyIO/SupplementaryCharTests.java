package easyIO;

import easyIO.regexp.Matcher;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/** Tests of characters outside the Basic Multilingual Plane, which next() reports
 *  as surrogate pairs and nextCodePoint() as single code points. */
public class SupplementaryCharTests {
    private static final String EMOJI = "😀"; // U+1F600
    private static final String INPUT = "a" + EMOJI + "b";

    private static BacktrackScanner scanner(String input) {
        return new BacktrackScanner(new StringReader(input));
    }

    @Test void nextReturnsSurrogatePair() throws EOF {
        BacktrackScanner b = scanner(INPUT);
        StringBuilder sb = new StringBuilder();
        while (b.hasNext()) sb.append(b.next());
        assertEquals(INPUT, sb.toString());
    }

    @Test void nextCodePointReturnsWholeCharacter() throws EOF {
        BacktrackScanner b = scanner(INPUT);
        assertEquals('a', b.nextCodePoint());
        assertEquals(0x1F600, b.nextCodePoint());
        assertEquals('b', b.nextCodePoint());
        assertFalse(b.hasNext());
    }

    @Test void peekBetweenHalves() throws EOF {
        BacktrackScanner b = scanner(EMOJI + "b");
        assertEquals(EMOJI.charAt(0), b.next());
        assertEquals(EMOJI.charAt(1), b.peek());
        assertEquals(EMOJI.charAt(1), b.next());
        assertEquals('b', b.peek());
    }

    @Test void readerInterface() throws Exception {
        BacktrackScanner b = scanner(INPUT);
        char[] buf = new char[10];
        int n = b.read(buf, 0, buf.length);
        assertEquals(INPUT, new String(buf, 0, n));
    }

    @Test void abortToMarkBetweenHalves() throws EOF {
        BacktrackScanner b = scanner(EMOJI + "b");
        assertEquals(EMOJI.charAt(0), b.next());
        b.mark();
        assertEquals(EMOJI.charAt(1), b.next());
        assertEquals('b', b.next());
        b.abort();
        assertEquals(EMOJI.charAt(1), b.next());
        assertEquals('b', b.next());
    }

    @Test void abortToMarkBeforePair() throws EOF {
        BacktrackScanner b = scanner(EMOJI + "b");
        b.mark();
        assertEquals(EMOJI.charAt(0), b.next());
        b.abort();
        assertEquals(0x1F600, b.nextCodePoint());
        assertEquals('b', b.next());
    }

    @Test void advanceSkipsWholeCharacter() throws EOF {
        BacktrackScanner b = scanner(INPUT);
        b.advance();
        b.advance();
        assertEquals('b', b.next());
    }

    @Test void stringWithSupplementaryCharacter() throws UnexpectedInput, EOF {
        BacktrackScanner b = scanner(INPUT + "c");
        b.string(INPUT);
        assertEquals('c', b.next());
    }

    @Test void scannerNextPattern() throws UnexpectedInput {
        Scanner sc = new Scanner(new StringReader(INPUT + " x"), "test");
        assertEquals(INPUT, sc.next(new Matcher("a" + EMOJI + "b")));
    }
}
