package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;

import java.io.StringReader;
import java.util.*;

import static easyIO.StdIO.print;
import static easyIO.StdIO.println;

public class Matcher {
    /// A potentially invalid state that the matcher went through at input position `inputPosition` while searching.
    private record IndexedState(int inputPosition, RegExp r) {}

    private static final boolean DEBUG = false;
    private Match match;
    private final RegExp expr;

    private Set<IndexedState> hopelessStates = null;
    public Matcher(RegExp r) {
        expr = r;
        expr.numberCaptures(0);
        match = new EmptyMatch(r);
    }
    /** Create a matcher based on a regular expression to be parsed.
     * Requires: the regular expression must be valid. */
    public Matcher(String pat) {
        try {
            expr = Parser.parse(pat);
        } catch (Parser.SyntaxError e) {
            throw new IllegalArgumentException("Invalid regular expression: " + pat);
        }
        match = new EmptyMatch(expr);
    }

    public static class FailedMatch extends Exception {
        static FailedMatch exception = new FailedMatch();
    }
    public Match match(String s) throws FailedMatch {
        return match(new BacktrackScanner(new BacktrackScanner.ReaderSource(new StringReader(s), s)));
    }
    private void reportState(int pos, Match match, int ch) {
        print(pos + ". State is now RE = " + this.match.regexp());
        if (ch != 0) println(", input character = " + ((char) ch));
        else println();
    }
    /** Whether the regular expression matches the entire input. */
    public Match match(BacktrackScanner s) throws FailedMatch {
        while (s.hasNext()) {
            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            if (DEBUG) reportState(s.inputPosition(), match, ch);
            match = match.scan(ch);
            if (match.regexp().isVoid()) {
                throw FailedMatch.exception;
            }
        }
        if (DEBUG) reportState(s.inputPosition(), state, 0);
        if (match.regexp().nullable())
            return match.finish();
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
        // This implementation avoids backtracking by memoizing hopeless states, so
        // it avoids the typical quadratic worst-case complexity of maximal munch tokenization.
        boolean success = false;
        state = new State(expr);
        if (hopelessStates == null) hopelessStates = new HashSet<>();
        Map<Integer, IndexedState> seenStates = new HashMap<>(); // states seen along the way
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
                state = re.scan(ch, state);

                seenStates.put(pos, new IndexedState(pos, state.re));
            } catch (EOF exc) {
                break;
            }
            if (state.re.nullable()) {
                success = true;
                s.mark(); // end mark, so we can roll back to this position
                end = s.inputPosition();
            } else if (state.re.isVoid() || hopelessStates.contains(new IndexedState(s.inputPosition(), state.re))) {
                if (DEBUG && hopelessStates.contains(new IndexedState(s.inputPosition(), state.re))) {
                    println("Abandoning early because state is hopeless");
                }
                if (success) {
                    int pos = s.inputPosition();
                    for (int i = end; i < pos; i++) {
                        IndexedState h = seenStates.get(i);
                        assert h != null;
                        hopelessStates.add(h);
                    }
                    // roll back to previous end mark
                    s.abort();
                    seenStates.clear();
                    return List.of(); // XXX needs something here.
                }
                s.abort(); // clear old start mark
                s.advance();
                s.mark(); // new start mark
                state = new State(expr);
            }
        }
        if (success) { // XXX this repeats code from above.
            int pos = s.inputPosition();
            for (int i = end; i < pos; i++) {
                IndexedState h = seenStates.get(i);
                assert h != null;
                hopelessStates.add(h);
            }
            // roll back to previous end mark
            s.abort();
            seenStates.clear();
            return List.of();
        } else {
            throw FailedMatch.exception;
        }
    }
}
