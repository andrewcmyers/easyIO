package easyIO.regexp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static easyIO.StdIO.println;

public abstract class RegExp {
    /** A memoization map from next characters (Unicode codepoints)
     * to Brzozowski derivatives: regular expressions recognizing
     * the suffix of the expression. */
    private final Map<Integer, RegExp> derivatives = new HashMap<>();
    static final HashMap<RegExp, RegExp> regExps = new HashMap<>();

    private static final boolean DEBUG = false;

    /** Whether this RE matches no strings. */
    public boolean isVoid() {
        return false;
    }
    /** Whether this RE matches the empty string */
    public boolean nullable() {
        return true;
    }

    /** Regular expressions are interned so that there is only one instance of
     * an equal regular expression. This is the canonical representation of the
     * regular expression.
     */
    static RegExp canonicalize(RegExp r) {
        RegExp result = regExps.get(r);
        if (result == null) {
            regExps.put(r,r);
            result = r;
        }
        return result;
    }
    /** The Brzozowski derivative of this regular expression. */
    public RegExp derivative(int codepoint) {
        RegExp derivative = derivatives.get(codepoint);
        if (derivative == null) {
            if (DEBUG) {
                println("Computing derivative of " + this + " (" + address() + ") with symbol " + Character.toString(codepoint));
            }
            derivative = canonicalize(computeDerivative(codepoint));
            derivatives.put(codepoint, derivative);
        }
        return derivative;
    }

    public Matcher.State scan(int codepoint, Match match) {
        var derivative = derivative(codepoint);
        return new Matcher.State(derivative, updateMatch(match, codepoint));
    }

    protected abstract Match updateMatch(Match match, int codepoint);

    /** The Brzozowski derivative of this regular expression, computed without
     * relying on the derivative cache to contain this derivative. */
    protected abstract RegExp computeDerivative(int codepoint);

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('/');
        appendString(b, 0);
        b.append('/');
        return b.toString();
    }

    /** Precedence: 0 = top level
     *  1 = |
     *  2 = concat
     *  3 = star
     */
    public abstract void appendString(StringBuilder b, int precedence);

    String address() {
        return super.toString();
    }
}
