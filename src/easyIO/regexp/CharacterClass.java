package easyIO.regexp;

import java.util.Arrays;

import static easyIO.regexp.StringRE.empty;
import static easyIO.regexp.StringRE.string;

/** A set of characters (Unicode codepoints), matching any one character in the set.
 *  The set is represented canonically as sorted, disjoint, non-adjacent ranges, so
 *  that equal sets are equal regular expressions; a negated class is stored as the
 *  complement of its ranges.
 */
public class CharacterClass extends RegExp {
    /** Range i is ranges[2i] .. ranges[2i+1], inclusive. */
    private final int[] ranges;
    private final int hash;

    static final int MAX = Character.MAX_CODE_POINT;

    private CharacterClass(int[] ranges) {
        this.ranges = ranges;
        hash = Arrays.hashCode(ranges);
    }

    /** A canonical RE matching any one character in the given ranges, or any one
     *  character not in them if negated. The ranges are given as pairs
     *  (start0, end0, start1, end1, ...), inclusive, in any order, possibly
     *  overlapping. An empty set is void, and a single character is a string. */
    public static RegExp of(int[] ranges, boolean negated) {
        int[] r = normalize(ranges);
        if (negated) r = complement(r);
        if (r.length == 0) return VoidRE.create();
        if (r.length == 2 && r[0] == r[1]) return string(Character.toString(r[0]));
        return canonicalize(new CharacterClass(r));
    }

    /** Any one character from start to end, inclusive. */
    public static RegExp range(int start, int end) {
        return of(new int[]{start, end}, false);
    }
    /** Any one character not from start to end. */
    public static RegExp excludeRange(int start, int end) {
        return of(new int[]{start, end}, true);
    }
    /** Any one character. */
    public static RegExp anyChar() {
        return of(new int[0], true);
    }
    /** Any one character except a line break ('\n' or '\r'). This is the meaning
     *  of '.' in a pattern. */
    public static RegExp anyButLineBreak() {
        return of(new int[]{'\n', '\n', '\r', '\r'}, true);
    }

    /** Sort and merge ranges, dropping empty ones. */
    private static int[] normalize(int[] ranges) {
        int n = ranges.length / 2;
        int[][] pairs = new int[n][];
        for (int i = 0; i < n; i++) pairs[i] = new int[]{ranges[2 * i], ranges[2 * i + 1]};
        Arrays.sort(pairs, (a, b) -> Integer.compare(a[0], b[0]));
        int[] out = new int[2 * n];
        int k = 0;
        for (int[] p : pairs) {
            if (p[0] > p[1]) continue;
            if (k > 0 && p[0] <= out[k - 1] + 1) {
                out[k - 1] = Math.max(out[k - 1], p[1]);
            } else {
                out[k++] = p[0];
                out[k++] = p[1];
            }
        }
        return Arrays.copyOf(out, k);
    }

    /** The complement of normalized ranges, within 0 .. MAX. */
    private static int[] complement(int[] r) {
        int[] out = new int[r.length + 2];
        int k = 0, next = 0;
        for (int i = 0; i < r.length; i += 2) {
            if (r[i] > next) { out[k++] = next; out[k++] = r[i] - 1; }
            next = r[i + 1] + 1;
        }
        if (next <= MAX) { out[k++] = next; out[k++] = MAX; }
        return Arrays.copyOf(out, k);
    }

    /** Whether codepoint is in this class. */
    public boolean contains(int codepoint) {
        // binary search for the last range starting at or before codepoint
        int lo = 0, hi = ranges.length / 2 - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (ranges[2 * mid] <= codepoint) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        return found >= 0 && codepoint <= ranges[2 * found + 1];
    }

    @Override
    public boolean nullable() {
        return false;
    }

    @Override
    public RegExp computeDerivative(int codepoint) {
        return contains(codepoint) ? empty() : VoidRE.create();
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        if (this == anyButLineBreak()) {
            b.append('.');
            return;
        }
        // Print whichever of the class and its complement is shorter.
        int[] r = ranges;
        int[] c = complement(ranges);
        b.append('[');
        if (c.length < r.length || (r.length > 0 && r[0] == 0)) {
            b.append('^');
            r = c;
        }
        for (int i = 0; i < r.length; i += 2) {
            appendChar(b, r[i]);
            if (r[i + 1] > r[i]) {
                if (r[i + 1] > r[i] + 1) b.append('-');
                appendChar(b, r[i + 1]);
            }
        }
        b.append(']');
    }

    /** Append a character in the syntax of a class, escaping it if necessary. */
    private static void appendChar(StringBuilder b, int c) {
        switch (c) {
            case ']', '[', '\\', '-', '^' -> b.append('\\').appendCodePoint(c);
            default -> StringRE.appendEscaped(b, c);
        }
    }

    @Override public boolean equals(Object o) {
        return o instanceof CharacterClass cc && Arrays.equals(ranges, cc.ranges);
    }
    @Override public int hashCode() {
        return hash;
    }
}
