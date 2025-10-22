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
 * A parser for turning regexp expressions into regexps
 */
public class Parser {
    public static class SyntaxError extends Exception {
        int pos;

        SyntaxError(BacktrackScanner b) {
            pos = b.inputPosition();
        }
    }
    public static RegExp parse(String s) throws SyntaxError {
        return parseAlts(new BacktrackScanner(new StringReader(s)));
    }

    public static RegExp parseAlts(BacktrackScanner b) throws SyntaxError {
        List<RegExp> concats = new ArrayList<>();
        concats.add(parseConcat(b));
        while (b.hasNext()) {
            if (b.peek() == ')') break;
            expect(b, '|');
            concats.add(parseConcat(b));
        }
        return alt(concats.toArray(RegExps));
    }

    public static RegExp parseConcat(BacktrackScanner b) throws SyntaxError {
        List<RegExp> stars = new ArrayList<>();
        stars.add(parseStar(b));
        while (b.hasNext()) {
            int ch = b.peek();
            if (ch == ')' || ch == '|') break;
            stars.add(parseStar(b));
        }
        return concat(stars.toArray(RegExps));
    }

    private static RegExp parseStar(BacktrackScanner b) throws SyntaxError {
        RegExp r = parseAtom(b);
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

    private static final String specialChars = "*|()[]";

    private static RegExp parseAtom(BacktrackScanner b) throws SyntaxError {
        try {
            if (!b.hasNext()) return empty();
            switch (b.peek()) {
                case ')':
                    return empty();
                case '(': {
                    b.next();
                    if (b.peek() == '(') {
                        b.next();
                        RegExp alts = parseAlts(b);
                        expect(b, ')');
                        expect(b, ')');
                        return capture(alts);
                    } else {
                        RegExp alts = parseAlts(b);
                        expect(b, ')');
                        return alts;
                    }
                }
                case '[': {
                    b.next();
                    boolean negated = (b.peek() == '^');
                    if (negated) b.next();
                    StringBuilder sb = new StringBuilder();
                    while (b.peek() != ']') {
                        sb.append(b.next());
                    }
                    expect(b, ']');
                    return empty(); // XXX finish this case
                }
                default: {
                    return string(Character.toString(b.next()));
                }
            }
        } catch (EOF e) {
            throw new RuntimeException(e);
        }
    }
}
