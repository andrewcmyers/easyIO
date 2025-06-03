import easyIO.BacktrackScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/** Test whether it implements Reader */
class ReaderTests {
    private BacktrackScanner scanner;

    @BeforeEach
    public void setUp() {
        String input = "Hello, world!";
        scanner = new BacktrackScanner(new BacktrackScanner(new StringReader(input)));
    }

    @Test
    void testReadSingleChar() throws IOException {
        int ch = scanner.read();
        assertEquals('H', ch);
        ch = scanner.read();
        assertEquals('e', ch);
    }

    @Test
    void testReadIntoBuffer() throws IOException {
        char[] buffer = new char[5];
        int read = scanner.read(buffer, 0, buffer.length);
        assertEquals(5, read);
        assertArrayEquals(new char[]{'H', 'e', 'l', 'l', 'o'}, buffer);
    }

    @Test
    void testMarkSupported() {
        assertTrue(scanner.markSupported(), "markSupported() should return true");
    }

    @Test
    void testMarkAndReset() throws IOException {
        assertTrue(scanner.markSupported());

        int firstChar = scanner.read();
        assertEquals('H', firstChar);

        scanner.mark(100); // mark after reading 'H'

        char[] buffer = new char[5];
        scanner.read(buffer, 0, 5); // read "ello,"

        assertArrayEquals(new char[]{'e', 'l', 'l', 'o', ','}, buffer);

        scanner.reset(); // go back to after 'H'

        int ch = scanner.read();
        assertEquals('e', ch); // should be 'e' again
    }

    @Test
    void testReadToEnd() throws IOException {
        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = scanner.read()) != -1) {
            result.append((char) ch);
        }
        assertEquals("Hello, world!", result.toString());
    }

    @Test
    void testResetWithoutMark() {
        assertThrows(IOException.class, () -> {
            scanner.reset(); // should throw if mark() wasn't called
        });
    }
}
