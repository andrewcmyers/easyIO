package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;

import java.io.StringReader;
import java.util.*;

import static easyIO.StdIO.print;
import static easyIO.StdIO.println;

public class Matcher {
    private static final boolean DEBUG = false;
    private final ArrayList<String> captures = new ArrayList<>();
    private StringBuilder currentCaptureBuilder = null;
    private Capture currentCapture = null;
    private State state;
    private RegExp expr;
    record Hopeless(int index, RegExp r) {}
    private Set<Hopeless> hopelessStates = null;
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
    private void reportState(int pos, State state, int ch) {
        print(pos + ". State is now RE=" + state.re + "  captures=" + captures + " current = " +
                (currentCapture == null ? "none" : currentCaptureBuilder.toString()));
        if (ch != 0) println(", input character = " + ((char) ch));
        else println();
    }
    /** Whether the regular expression matches the entire input. */
    public List<String> match(BacktrackScanner s) throws FailedMatch {
        while (s.hasNext()) {
            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            if (DEBUG) reportState(s.inputPosition(), state, ch);
            if (currentCapture != null) currentCaptureBuilder.appendCodePoint(ch);
            state = state.re.scan(ch, this);
            if (state.re.isVoid()) {
                throw FailedMatch.exception;
            }
        }
        if (DEBUG) reportState(s.inputPosition(), state, 0);
        if (state.re.nullable())
            return captures;
        else
            throw FailedMatch.exception;
    }

    /** Find the first occurrence of the entire regular expression on the input.
     *  A mark is inserted at the beginning of the occurrence and the scanner position
     *  is left at the end of the longest string that matches the RE, so that the client
     *  can choose to accept or abort the match. Captured strings passed along the way
     *  are returned. If no occurrence is found, the scanner goes to the end of the input.
     */
    public List<String> search(BacktrackScanner s) throws FailedMatch {
        boolean success = false;
        state = new State(expr);
        if (hopelessStates == null) hopelessStates = new HashSet<>();
        Map<Integer,Hopeless> seenStates = new HashMap<>();
        s.mark(); // start mark
        int end = s.inputPosition();
        while (s.hasNext()) {
            // if success:  ---------^---------^--------|------
            //                     start.     end.     current
            //
            //.if no success: --------^------------------|-----
            //                      start              current
            RegExp re = state.re;
            try {
                int pos = s.inputPosition();
                int ch = s.nextCodePoint();
                if (DEBUG) reportState(pos, state, ch);
                state = re.scan(ch, this);

                seenStates.put(pos, new Hopeless(pos, state.re));
            } catch (EOF exc) {
                break;
            }
            if (state.re.nullable()) {
                success = true;
                s.mark(); // end mark, so we can roll back to this position
                end = s.inputPosition();
            } else if (state.re.isVoid() || hopelessStates.contains(new Hopeless(s.inputPosition(), state.re))) {
                if (DEBUG && hopelessStates.contains(new Hopeless(s.inputPosition(), state.re))) {
                    println("Abandoning early because state is hopeless");
                }
                if (success) {
                    int pos = s.inputPosition();
                    for (int i = end; i < pos; i++) {
                        Hopeless h = seenStates.get(i);
                        assert h != null;
                        hopelessStates.add(h);
                    }
                    // roll back to previous end mark
                    s.abort();
                    seenStates.clear();
                    return captures;
                }
                s.abort(); // clear old start mark
                s.advance();
                s.mark(); // new start mark
                state = new State(expr);
            }
        }
        if (success) {
            int pos = s.inputPosition();
            for (int i = end; i < pos; i++) {
                Hopeless h = seenStates.get(i);
                assert h != null;
                hopelessStates.add(h);
                println("Hopeless state: " + h);
            }
            // roll back to previous end mark
            s.abort();
            seenStates.clear();
            return captures;
        } else {
            throw FailedMatch.exception;
        }
    }
}
