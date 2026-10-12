package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static easyIO.regexp.Capture.capture;
import static easyIO.regexp.Concat.concat;
import static easyIO.regexp.RegExp.RegExps;
import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.StarRE.star;
import static easyIO.regexp.StringRE.empty;
import static easyIO.regexp.StringRE.string;

/**
 * A parser for turning regexp expressions into regexps.
 *
 * Syntax:
 *   R|S     alternation
 *   RS      concatenation
 *   R*      Kleene star
 *   (R)     grouping, without capturing
 *   {R}     capture. Captures are numbered 1, 2, ... in the order their opening
 *           braces appear, and cannot occur inside a Kleene star.
 *   [...]   character class: any one of the characters listed, where c-d is a
 *           range of characters. [^...] is any character not listed. A '-'
 *           at the start or end is literal; other characters can be made
 *           literal with a backslash, e.g. [\]\-]. [] matches nothing and [^]
 *           matches any character.
 *   .       any character except a line break: the same as [^\n\r]
 *   \n \r \t newline, carriage return, tab
 *   \c      the character c literally, for any c that is not a letter or digit
 *           (e.g., \* or \{ or \.). A backslash before any other letter or digit
 *           is a syntax error, reserving it for future use.
 *   c       any other character, literally
 */
public class Parser {
    public static class SyntaxError extends Exception {
        int pos;

        SyntaxError(BacktrackScanner b) {
            pos = b.inputPosition();
        }
    }
    /** Parse a regular expression. Captures are numbered 1, 2, ... in the order
     *  their opening braces appear. */
    public static RegExp parse(String s) throws SyntaxError {
        BacktrackScanner b = new BacktrackScanner(new StringReader(s));
        RegExp r = parseAlts(b);
        // parseAlts stops at an unmatched ')' or '}'; the whole pattern must be consumed.
        if (b.hasNext()) throw new SyntaxError(b);
        return r;
    }

    public static RegExp parseAlts(BacktrackScanner b) throws SyntaxError {
        return parseAlts(b, new int[]{1});
    }

    public static RegExp parseConcat(BacktrackScanner b) throws SyntaxError {
        return parseConcat(b, new int[]{1});
    }

    /** nextCapture[0] is the index of the next capture to be parsed. */
    private static RegExp parseAlts(BacktrackScanner b, int[] nextCapture) throws SyntaxError {
        List<RegExp> concats = new ArrayList<>();
        concats.add(parseConcat(b, nextCapture));
        while (b.hasNext()) {
            if (b.peek() == ')' || b.peek() == '}') break;
            expect(b, '|');
            concats.add(parseConcat(b, nextCapture));
        }
        return alt(concats.toArray(RegExps));
    }

    private static RegExp parseConcat(BacktrackScanner b, int[] nextCapture) throws SyntaxError {
        List<RegExp> stars = new ArrayList<>();
        stars.add(parseStar(b, nextCapture));
        while (b.hasNext()) {
            int ch = b.peek();
            if (ch == ')' || ch == '}' || ch == '|') break;
            stars.add(parseStar(b, nextCapture));
        }
        return concat(stars.toArray(RegExps));
    }

    private static RegExp parseStar(BacktrackScanner b, int[] nextCapture) throws SyntaxError {
        RegExp r = parseAtom(b, nextCapture);
        try {
            if (b.peek() == '*') {
                r = star(r);
            }
            while (b.peek() == '*') { b.next(); }
        } catch (EOF e) {
            throw new SyntaxError(b);
        }
        return r;
    }

    private static void expect(BacktrackScanner b, int codepoint) throws SyntaxError {
        if (b.peek() != codepoint) throw new SyntaxError(b);
        try {
            b.next();
        } catch (EOF e) {
            throw new Error("Cannot happen");
        }
    }


    private static RegExp parseAtom(BacktrackScanner b, int[] nextCapture) throws SyntaxError {
        try {
            if (!b.hasNext()) return empty();
            switch (b.peek()) {
                case ')':
                case '}':
                    return empty();
                case '(': {
                    b.next();
                    RegExp alts = parseAlts(b, nextCapture);
                    expect(b, ')');
                    return alts;
                }
                case '{': {
                    b.next();
                    int k = nextCapture[0]++;
                    RegExp alts = parseAlts(b, nextCapture);
                    expect(b, '}');
                    return capture(alts, k);
                }
                case '[': {
                    b.next();
                    return parseClass(b);
                }
                case '*':
                case ']':
                    throw new SyntaxError(b);
                case '.': {
                    b.next();
                    return CharacterClass.anyButLineBreak();
                }
                case '\\':
                    return string(Character.toString(escape(b)));
                default: {
                    return string(Character.toString(b.nextCodePoint()));
                }
            }
        } catch (EOF e) {
            throw new RuntimeException(e);
        }
    }

    /** Parse the rest of a character class, after its opening '['. */
    private static RegExp parseClass(BacktrackScanner b) throws SyntaxError, EOF {
        boolean negated = b.peek() == '^';
        if (negated) b.next();
        List<Integer> ranges = new ArrayList<>();
        while (true) {
            if (!b.hasNext()) throw new SyntaxError(b);
            if (b.peek() == ']') break;
            int start = classChar(b);
            int end = start;
            // a '-' between two characters makes a range; otherwise it is literal
            if (b.peek() == '-') {
                b.mark();
                b.next();
                if (b.hasNext() && b.peek() != ']') {
                    b.accept();
                    end = classChar(b);
                    if (end < start) throw new SyntaxError(b);
                } else {
                    b.abort();
                }
            }
            ranges.add(start);
            ranges.add(end);
        }
        expect(b, ']');
        return CharacterClass.of(ranges.stream().mapToInt(Integer::intValue).toArray(), negated);
    }

    /** Parse one character of a character class, which may be escaped with a backslash. */
    private static int classChar(BacktrackScanner b) throws SyntaxError, EOF {
        if (b.peek() == '\\') return escape(b);
        return b.nextCodePoint();
    }

    /** Parse an escape sequence: a backslash and the character after it, and
     *  return the character it denotes. */
    private static int escape(BacktrackScanner b) throws SyntaxError, EOF {
        b.next(); // the backslash
        if (!b.hasNext()) throw new SyntaxError(b);
        int c = b.peek();
        int result = switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> Character.isLetterOrDigit(c) ? -1 : c;
        };
        if (result == -1) throw new SyntaxError(b); // reserved
        b.nextCodePoint();
        return result;
    }
}
