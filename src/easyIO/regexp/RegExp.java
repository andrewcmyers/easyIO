package easyIO.regexp;

import java.util.HashMap;
import java.util.Map;

import static easyIO.StdIO.println;

/** A regular expression, with support for matching and searching an input sourc, based on
 * Brzozowski derivatives.
 */
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
     * an equal regular expression. This is the canonical instance representing
     * that regular expression. The factory methods for regular expressions all
     * return canonical instances.
     */
    static RegExp canonicalize(RegExp r) {
        RegExp result = regExps.get(r);
        if (result == null) {
            regExps.put(r,r);
            result = r;
        }
        return result;
    }

    /** The (canonical) Brzozowski derivative of this regular expression. */
    public RegExp derivative(int codepoint) {
        RegExp derivative = derivatives.get(codepoint);
        if (derivative == null) {
            if (DEBUG) {
                println("Computing derivative of " + this + " (" + address() + ") with symbol " + Character.toString(codepoint));
            }
            derivative = computeDerivative(codepoint);
            derivatives.put(codepoint, derivative);
        }
        return derivative;
    }

    /**
     * Scan a single codepoint and return the updated matcher state.
     */
    public Matcher.State scan(int codepoint, Matcher matcher) {
        var derivative = derivative(codepoint);
        return new Matcher.State(derivative);
    }

    /** The Brzozowski derivative of this regular expression, computed without
     * relying on the derivative cache to contain this derivative. It is provided
     * by subclasses. It always produces a canonical RE.
     */
    protected abstract RegExp computeDerivative(int codepoint);

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('/');
        appendString(b, 0);
        b.append('/');
        return b.toString();
    }

    /** Append a string representation of the regular expression to
     * the builder b. The precedence of the surrounding context is provided
     * so that necessary parentheses can be added.
     * Precedences: 0 = top level
     *              1 = |
     *              2 = concat
     *              3 = star
     */
    public abstract void appendString(StringBuilder b, int precedence);

    /**
     * The Object.toString() output, for use in debugging.
     */
    String address() {
        return super.toString();
    }
    public static final RegExp[] RegExps = {};
}
