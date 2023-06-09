package easyIO;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

/**
 * A scanner class that, unlike {@code java.util.Scanner}, supports arbitrary
 * lookahead and backtracking. The caller can use {@code mark()} to set some
 * number of marks in the input stream, then {@code accept()} to erase the
 * previous mark or {@code abort()} to roll back to (and erase) the previous
 * mark. The marks function as a stack of previous points in the input.
 *
 * The class also allows reading a stream that is spread across multiple input
 * sources, and keeps track of the current source, current line number, and
 * current position within the line.
 *
 * Arbitrary lookahead is allowed, but the space consumed by a scanner is
 * proportional to the number of chars between the first mark and last
 * lookahead position.
 *
 * @see easyIO.Scanner easyIO.Scanner
 */
public class BacktrackScanner {

     /** A source of input for the scanner. */
     interface Source {
         /** The name of the source, for diagnostic purposes. */
         String name();
         /** The current line number of the source */
         int lineNo();
         /** The current column of the source */
         int column();
         /** Return the next character location or null if eof is reached. */
         Location read() throws IOException;
         void close() throws IOException;
     }

    /** A source of input for the scanner in which the input comes from a Reader. */
     private static class ReaderSource implements Source {
        private String name;
        private Reader reader;
        private int lineno = 1;
        private int charpos = 0;
        /** A pending character to be delivered on next read(), if non-null. (Used for low surrogates) */
        private Location pending = null;

        public ReaderSource(Reader r, String n) {
            name = n;
            reader = r;
        }
        @Override
        public String toString() {
            return "\"" + name + "\", line " + lineno + ", character " + charpos;
        }
        @Override
        public String name() { return name; }
        @Override
        public int lineNo() {
            return lineno;
        }
        @Override
        public int column() {
            return charpos;
        }

        /** Return the next character location or null if eof is reached. */
        public Location read() throws IOException {
            if (pending != null) {
                Location result = pending;
                pending = null;
                return result;
            }
            int c = reader.read();
            if (c == -1) return null;
            if (c == '\n') {
                lineno++;
                charpos = 0;
            } else {
                if (Character.isHighSurrogate((char) c)) {
                    int lowS = reader.read();
                    if (Character.isLowSurrogate((char) lowS)) {
                        c = Character.toCodePoint((char) c, (char) lowS);
                    } else {
                        if (lowS != -1) {
                            pending = new Location(this, lineno, charpos, (char) lowS);
                        }
                    }
                }
                charpos++;
            }
            return new Location(this, lineno, charpos, c);
        }
        public void close() throws IOException {
            reader.close();
        }
    }

    /**
     * An input character along with information about the source of the
     * character, and its line number and position within the line.
     */
    public static class Location {
        public Location(Source i, int line, int column, int ch) {
            input = i;
            lineno = line;
            charpos = column;
            character = ch;
            assert ch >= 0;
        }
        Source input;
        int lineno;
        int charpos;
        int character;
        public String toString() {
            String cs = String.valueOf((char) character);
            if (character < ' ') {
                String hex = "0123456789abcdef";
                switch (character) {
                    case '\n': cs = "\\n"; break;
                    case '\t': cs = "\\t"; break;
                    case '\r': cs = "\\r"; break;
                    case '\b': cs = "\\b"; break;
                    default:
                        cs = "\\x{" + hex.charAt(character/16) + hex.charAt(character % 16) + "}";
                }
            }
            return '"' + input.name() + "\", line " +
                    lineno + ", char " + charpos + " (" + cs + ")";
        }
        public int lineNo() {
            return lineno;
        }
        public int column() {
            return charpos;
        }
    }

    LinkedList<Source> inputs = new LinkedList<>();

    /** Input from suspended sources that should be restored once this input completes. */
    LinkedList<Location[]> suspendedInput = new LinkedList<>();
    Location[] buffer;
    int pos; // current input position (always in the deepest region)
    int end; // marks end of characters actually read in buffer (last is at end-1)

    /** if non-zero, pendingChar is the low surrogate for a character whose
     *  high surrogate has already been returned by read()
     */
    char pendingChar = 0;

    /** The stack of regions represented by their positions within prefix. Indices
     *  must increase monotonically.
     *       prefix            unread input (from linked list)
     *  [...[....[...^....     .........$
     *  0   1    2   pos  end           eof
     *  marks
     */
    int[] marks;
    int nmarks;
    static final int INITIAL_SIZE = 1;

    public boolean invariant() {
        assert nmarks >= 0;
        assert end >= 0 && end <= buffer.length;
        assert nmarks == 0 || pos >= marks[nmarks-1] && pos <= end;
        for (int i = 0; i < nmarks; i++) {
            assert marks[i] <= pos;
            assert i <= 0 || marks[i] >= marks[i - 1];
        }
        return true;
    }
    /** Dump the state of the scanner to w in a human-readable form. */
    public void dump(StringBuilder w) {
        w.append("[Scanner buffer length=").append(buffer.length)
         .append(" pos=").append(pos)
         .append(" end=").append(end)
         .append(" nmarks=").append(nmarks);
        if (nmarks > 0) {
            for (int i = 0; i < nmarks; i++) {
                w.append(i == 0 ? " [" : ", ").append(marks[i]);
            }
            w.append(']');
        }

        if (pos < end) w.append("\ncurrent = ").append(buffer[pos]);
        w.append(" \nCurrent source: ").append(source()).append("\nBuffer: ");

        int m = 0;
        int start = nmarks > 0 ? marks[0] : pos;
        start = (start >= 5) ? start - 5 : 0;
        for (int i = start; i < end; i++) {
            while (m < nmarks && marks[m] == i) {
                m++;
                w.append("[");
            }
            if (i == pos) w.append("^");
            w.appendCodePoint(buffer[i].character);
        }
        if (pos == end) w.append("^");
        w.append("...\n");
    }

    public BacktrackScanner() {
        buffer = new Location[INITIAL_SIZE];
        end = 0;
        marks = new int[INITIAL_SIZE];
        nmarks = 0;
    }

    public void close() throws IOException {
        for (Source i : inputs) {
            i.close();
        }
    }
    public String source() {
        return inputs.getFirst().name();
    }
    /** The current line number. Line numbers start from 1. */
    public int lineNo() {
        return inputs.getFirst().lineNo();
    }
    /** See {@code column} */
    @Deprecated
    public int charPos() {
        return column();
    }
    /** The current column number. Column numbers for printable characters start from 1,
     *  with newlines occurring at column 0. */
    public int column() {
        try {
            return location().column();
        } catch (EOF e) {
            return inputs.getFirst().column();
        }
    }

    /** Add r to the input stream ahead of any existing inputs.*/
    public void includeSource(Reader r, String name) {
        Source i = new ReaderSource(r, name);
        inputs.addFirst(i);
        Location[] suspended = new Location[end-pos];
        System.arraycopy(buffer, pos, suspended, 0, end-pos);
        suspendedInput.addFirst(suspended);
        end = pos;
    }
    /** Add r to the input stream after existing inputs. */
    public void appendSource(Reader r, String name) {
        Source i = new ReaderSource(r, name);
        inputs.addLast(i);
        suspendedInput.addLast(new Location[0]);
    }

    /** Whether there are characters already read ahead of the current position. */
    boolean charsAhead() {
        return (end - pos > 0);
    }

    /** Whether there is a character ahead in input. */
    public boolean hasNext() {
        return (peek() != -1);
    }

    static final EOF eof = new EOF();
    static final UnexpectedInput uinp = new UnexpectedInput();

    /** The next character ahead in the input. Equivalent to {@code begin(); c = nextCodePoint(); abort(); return c;}
     *  except that it returns -1 if the end of input has been reached. */
    public int peek() {
        if (charsAhead())
            return buffer[pos].character;
        Location c;
        if (inputs.isEmpty()) return -1;
        try {
            c = inputs.getFirst().read();
        } catch (IOException e) {
            c = null;
        }

        if (c == null) {
            Source fst = removeInput();
            try {
                fst.close();
            } catch (IOException e) {
                // It's only being read from so harmless to ignore?
            }
            if (inputs.size() == 0) return -1;
            return peek();
        }

        append(c);
        assert invariant();
        return c.character;
    }

    private Source removeInput() {
        Source result = inputs.removeFirst();
        assert pos == end; // buffer must be empty
        Location[] suspended = suspendedInput.removeFirst();
        for (Location l : suspended) append(l);
        return result;
    }

    private void append(Location loc) {
        int n = buffer.length;
        assert end <= n;
        if (end == n) {
            grow();
        }
        buffer[end++] = loc;
        assert invariant();
    }

    /**
     * Allocate a new prefix array at least twice as big as what is known to be
     * needed, and copy all active input to that array. Anything before the
     * first mark (or the current position if there is no mark) is discarded to
     * save space.
     */
    private void grow() {
        int start = pos;
        if (nmarks != 0) start = marks[0];
        int newlen = end - start;
        Location[] np;
        if (newlen * 2 < buffer.length) {
            np = buffer;
        } else {
            np = new Location[newlen * 2];
        }
        System.arraycopy(buffer, start, np, 0, newlen);
        buffer = np;
        for (int i = 0; i < nmarks; i++) {
            marks[i] -= start;
        }
        pos -= start;
        end = newlen;
    }

    /** Location in input source of the current position. */
    public Location location() throws EOF {
        if (pos == end) peek();
        if (pos < buffer.length) return buffer[pos];
        else throw eof;
    }
    /** Location in input source of the last mark. */
    public Location getMarkLocation() {
        return buffer[marks[nmarks-1]];
    }

    /** Add a mark at the current position. */
    public void mark() {
        assert invariant();
        if (nmarks == marks.length) {
            int[] rs2 = new int[nmarks*2];
            System.arraycopy(marks,  0,  rs2,  0,  nmarks);
            marks = rs2;
        }
        marks[nmarks++] = pos;
    }

    /** Effect: Erase the previous mark from the input, effectively
     *  accepting all input up to the current position. */
    public void accept() {
        assert nmarks > 0 && invariant();
        nmarks--;
    }
    /** The current number of marks. Exposed for use in assertions, so
     * client code can check that matching mark()...accept() calls occur
     * at the same depth.
     */
    public int depth() {
        return nmarks;
    }
    /** Return a string containing the characters from the most recent mark to the current position. */

    public String getToken() {
        assert nmarks > 0 && invariant();
        StringBuilder r = new StringBuilder();
        int s = marks[nmarks-1];
        for (int j = s; j < pos; j++) {
            r.appendCodePoint(buffer[j].character);
        }
        return r.toString();
    }

    /**
     * Roll the input position back to the most recent mark, and erase the mark,
     * effectively restarting scanning from that position.
     */
    public void abort() {
        assert nmarks > 0;
        pos = marks[nmarks-1];
        nmarks--;
    }

    /** Advance past the next character, if any. Do nothing if at end of input. */
    public void advance() {
        try {
            next();
        } catch (EOF e) {
            // do nothing
        }
    }

    public final boolean lowSurrogate(int ch) {
        return (ch >= 0xDC00 && ch <= 0xDFFF);
    }

    /** Read the next character from the stream. Throw EOF
     *  if there is no next character. If the next character is
     *  a Unicode supplementary character, it is reported by this
     *  method as two chars in sequence, representing a surrogate pair.
     */
    public char next() throws EOF {
        int ch = nextCodePoint();
        if (Character.charCount(ch) == 1)
            return (char) ch;
        char[] chs = Character.toChars(ch);
        pendingChar = chs[1];
        return chs[0];
    }

    /** Read the next character from the stream. Throw EOF
     *  if there is no next character. If the next character is
     *  a Unicode supplementary character, it is reported by this
     *  method as two chars in sequence, representing a surrogate pair.
     */
    public int nextCodePoint() throws EOF {
        if (charsAhead()) {
            return buffer[pos++].character;
        }
        try {
            if (inputs.size() == 0) throw eof;
            Location c = inputs.getFirst().read();
            if (c == null) {
                inputs.removeFirst();
                if (inputs.size() > 0) return nextCodePoint();
                throw eof;
            }
            append(c);
            pos++;
            return c.character;
        } catch (IOException e) {
            throw eof;
        }
    }

    /** Scan the characters of string s from the input.
     * @throws UnexpectedInput if something other than the expected characters
     *                         are encountered.
     */
    public void string(String s) throws UnexpectedInput {
        mark();
        for (int i = 0; i < s.length(); i++) {
            if (peek() == s.charAt(i)) {
                advance();
            } else {
                abort();
                throw uinp;
            }
        }
        accept();
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder();
        dump(b);
        return b.toString();
    }
}