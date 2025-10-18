package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;

import java.io.StringReader;

import static easyIO.StdIO.print;
import static easyIO.StdIO.println;

public class Matcher {
    private final RegExp expr;
    private static final boolean DEBUG = false;
    public Matcher(RegExp r) {
        expr = r;
    }
    public record State (
        RegExp re,
        Match match
    ){}
    public static class FailedMatch extends Exception {
        static FailedMatch exception = new FailedMatch();
    }
    public Match match(String s) throws FailedMatch {
        return match(new BacktrackScanner(new BacktrackScanner.ReaderSource(new StringReader(s), s)));
    }
    public Match match(BacktrackScanner s) throws FailedMatch {
        State state = new State(expr, new Discard());
        while (s.hasNext()) {
            if (DEBUG) print("State is now RE=" + state.re + "  match=" + state.match);
            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            if (DEBUG) println(", input character = " + ((char) ch));
            state = state.re.scan(ch, state.match);
            if (state.re.isVoid()) {
                throw FailedMatch.exception;
            }
        }
        if (DEBUG) println("Final state is RE = " + state.re + "  match = " + state.match);
        if (state.re.nullable())
            return state.match;
        else
            throw FailedMatch.exception;
    }
    public Match search(BacktrackScanner s) throws FailedMatch {
        State state = new State(expr, new Discard());
        boolean success = false;
        while (s.hasNext()) {
            print("State is now " + state.re + "," + state.match);
            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            println(", input character = " + ((char) ch));
            state = state.re.scan(ch, state.match);
            if (state.re.nullable()) {
                success = true;
                s.mark();
            } else if (state.re.isVoid()) {
                if (success) {
                    s.abort();
                    return state.match;
                }
                state = new State(expr, new Discard());
            }
        }
        if (success) {
            s.abort();
            return state.match;
        } else {
            throw FailedMatch.exception;
        }
    }
}
