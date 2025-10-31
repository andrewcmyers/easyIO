package easyIO.regexp;

import easyIO.regexp.Matcher.FailedMatch;
import easyIO.regexp.Matcher.State;

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
    private final Map<Integer, DerivativeResult> derivatives = new HashMap<>();
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

    public interface MatchTransformer {
        Match transform(Match input);
    }
    MatchTransformer sameMatch = m -> m;
    public record DerivativeResult(RegExp derivative, MatchTransformer transformer) {}

    /** The (canonical) Brzozowski derivative of this regular expression. */
    public DerivativeResult derivative(int codepoint) {
        var result = derivatives.get(codepoint);
        if (result == null) {
            if (DEBUG) {
                println("Computing derivative of " + this + " (" + address() + ") with symbol " + Character.toString(codepoint));
            }
            result = computeDerivative(codepoint);
            derivatives.put(codepoint, result);
        }
        return result;
    }

    /**
     * Scan a single codepoint and return the updated matcher state.
     */
    public State scan(int codepoint, State state) {
        var derivativeResult = derivative(codepoint);
        var match = derivativeResult.transformer().transform(state.match()).accept(codepoint);
        return new State(derivativeResult.derivative, match);
    }

//    protected Match updateMatch(Match match, int codepoint) {
//        return match;
//    }

    /// Number the captures under this node, starting from `index`. Return
    /// the first unused index at least `index`.
    /// unless index == -1. Default behavior: do nothing, return the same index.
    protected int numberCaptures(int index) {
        return index;
    }

    /** The Brzozowski derivative of this regular expression, computed without
     * relying on the derivative cache to contain this derivative. It is provided
     * by subclasses. It always produces a canonical RE.
     *
     * For most REs, the 'match' parameter does not matter.
     */
    protected abstract DerivativeResult computeDerivative(int codepoint);

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
    public static final DerivativeResult[] DerivativeResults = {};
    public static final MatchTransformer[] Transformers = {};

    public Match finish(Match match) throws FailedMatch {
        if (!this.nullable()) throw FailedMatch.exception;
        return match;
    }
}
