package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static easyIO.StdIO.print;
import static easyIO.StdIO.println;

public class Matcher {
    private static final boolean DEBUG = false;
    private final ArrayList<String> captures = new ArrayList<>();
    private StringBuilder currentCaptureBuilder = null;
    private Capture currentCapture = null;
    private State state;
    private RegExp expr;
    public Matcher(RegExp r) {
        expr = r;
        state = new State(r);
    }

    public void startCapture(Capture c) {
        if (currentCapture == c) return;
        currentCapture = c;
        currentCaptureBuilder = new StringBuilder();
    }
    public void finishCapture() {
        captures.add(currentCaptureBuilder.toString());
        currentCapture = null;
        currentCaptureBuilder = null;
    }

    public record State (
        RegExp re
    ){}

    public static class FailedMatch extends Exception {
        static FailedMatch exception = new FailedMatch();
    }
    public List<String> match(String s) throws FailedMatch {
        return match(new BacktrackScanner(new BacktrackScanner.ReaderSource(new StringReader(s), s)));
    }
    private void reportState(State state, int ch) {
        print("State is now RE=" + state.re + "  captures=" + captures + " current = " +
                (currentCapture == null ? "none" : currentCaptureBuilder.toString()));
        if (ch != 0) println(", input character = " + ((char) ch));
        else println();
    }
    public List<String> match(BacktrackScanner s) throws FailedMatch {
        while (s.hasNext()) {
            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            if (DEBUG) reportState(state, ch);
            if (currentCapture != null) currentCaptureBuilder.appendCodePoint(ch);
            state = state.re.scan(ch, this);
            if (state.re.isVoid()) {
                throw FailedMatch.exception;
            }
        }
        if (DEBUG) reportState(state, 0);
        if (state.re.nullable())
            return captures;
        else
            throw FailedMatch.exception;
    }
    public List<String> search(BacktrackScanner s) throws FailedMatch {
        boolean success = false;
        while (s.hasNext()) {

            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            reportState(state, ch);

            state = state.re.scan(ch, this);
            if (state.re.nullable()) {
                success = true;
                s.mark();
            } else if (state.re.isVoid()) {
                if (success) {
                    s.abort();
                    return captures;
                }
                state = new State(expr);
            }
        }
        if (success) {
            s.abort();
            return captures;
        } else {
            throw FailedMatch.exception;
        }
    }
}
