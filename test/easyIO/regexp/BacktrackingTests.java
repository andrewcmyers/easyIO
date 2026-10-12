package easyIO.regexp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Patterns on which java.util.regex, a backtracking matcher, takes exponential
 *  time but this package takes linear time.
 *
 *  The pattern (a|(a|aa)*b)*c on a string of n a's has no match, but there are
 *  Fibonacci(n)-many ways to split the a's among the alternatives before the
 *  missing c is discovered. A backtracking matcher tries them all. Here the
 *  derivatives of the pattern collapse to a few states, so each input symbol
 *  costs constant time.
 */
public class BacktrackingTests {
    /** A CharSequence that counts how many times its characters are read, as a
     *  deterministic measure of the work done by java.util.regex. */
    private static final class CountingSequence implements CharSequence {
        private final String s;
        long reads = 0;

        CountingSequence(String s) { this.s = s; }
        @Override public char charAt(int i) { reads++; return s.charAt(i); }
        @Override public int length() { return s.length(); }
        @Override public CharSequence subSequence(int start, int end) { return s.subSequence(start, end); }
        @Override public String toString() { return s; }
    }

    private static long javaReads(Pattern p, String input) {
        CountingSequence cs = new CountingSequence(input);
        assertFalse(p.matcher(cs).matches());
        return cs.reads;
    }

    @Test void javaIsExponential() {
        // Each 5 more a's multiplies the work by about 11 (the golden ratio to the
        // 5th power). The bound is loose so that the test does not depend on the
        // details of the JDK's matcher, only on its backtracking.
        Pattern p = Pattern.compile("(a|(a|aa)*b)*c");
        long previous = javaReads(p, "a".repeat(5));
        for (int n = 10; n <= 20; n += 5) {
            long reads = javaReads(p, "a".repeat(n));
            assertTrue(reads > 5 * previous,
                    "expected exponential growth at n=" + n + ": " + previous + " -> " + reads);
            previous = reads;
        }
    }

    @Test void oursIsLinear() {
        // At this length a backtracking matcher would need on the order of
        // 1.6^100000 steps; Java already takes seconds at n = 35.
        Matcher m = new Matcher("(a|(a|aa)*b)*c");
        String input = "a".repeat(100_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(Matcher.FailedMatch.class, () -> m.match(input)));
    }

    @Test void oursIsLinearWithCaptures() {
        // The same pattern with a capture around the starred part: on success
        // there is a single match despite the exponentially many parses, and on
        // failure it is still fast.
        Matcher m = new Matcher("{(a|(a|aa)*b)*}c");
        String as = "a".repeat(100_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            List<Match> ms = m.match(as + "c");
            assertEquals(1, ms.size());
            assertEquals(as, ms.getFirst().group(1));
            assertThrows(Matcher.FailedMatch.class, () -> m.match(as));
        });
    }
}
