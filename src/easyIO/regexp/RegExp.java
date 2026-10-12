package easyIO.regexp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static easyIO.StdIO.println;

/** A regular expression, with support for matching and searching an input source, based on
 * Brzozowski derivatives.
 *
 * Captures are handled by factoring the idealized derivative into a skeleton (this RE,
 * with captured strings erased) and capture state kept on the side. A derivative of a
 * skeleton term is a list of (term, ops) pairs: each ops says how the environments of
 * this term produce environments for the resulting term. See notes2.txt.
 */
public abstract class RegExp {
    /** A memoization map from next characters (Unicode codepoints)
     * to Brzozowski derivatives: regular expressions recognizing
     * the suffix of the expression. */
    private final Map<Integer, RegExp> derivatives = new ConcurrentHashMap<>();
    /** Memoized derivatives for ASCII codepoints, which avoid boxing and hashing.
     *  Allocated on first use.
     *
     *  Thread safety: the interned REs and their caches are shared by all threads.
     *  The maps are concurrent. The arrays and the other lazily computed fields are
     *  accessed racily, which is benign: a thread that misses an entry just computes
     *  it again, getting the same canonical result, and everything stored in them is
     *  immutable and published through final fields (or, for acceptOps, a volatile
     *  field), so a racing reader sees either null or a fully constructed value. */
    private RegExp[] asciiDerivatives = null;
    /** The number of ASCII codepoints, which are cached in arrays rather than maps. */
    static final int ASCII = 128;
    /** Memoized derivatives as lists of (term, ops) pairs. Only used for REs with captures. */
    private final Map<Integer, List<DerivTerm>> termDerivatives = new ConcurrentHashMap<>();
    /** Memoized accepting ops. */
    private List<Ops> nullOps = null;
    /** Memoized transitions when this RE is used as a matcher state, for ASCII
     *  codepoints and for all others. Allocated on first use. */
    Matcher.Transition[] asciiTransitions = null;
    final Map<Integer, Matcher.Transition> transitions = new ConcurrentHashMap<>();
    /** Memoized accepting ops for each position when this RE is used as a matcher state.
     *  Volatile so that a reader that sees the outer array also sees the inner ones. */
    volatile Ops[][] acceptOps = null;

    static final Map<RegExp, RegExp> regExps = new ConcurrentHashMap<>();

    private static final boolean DEBUG = false;

    /** One summand of a derivative: a skeleton term and the tag operations that
     *  produce its environments from the environments of the source term. */
    public record DerivTerm(RegExp term, Ops ops) {}

    /** Whether this RE matches no strings. */
    public boolean isVoid() {
        return false;
    }
    /** Whether this RE matches the empty string */
    public boolean nullable() {
        return true;
    }
    /** Whether this RE contains any capture, started or not. */
    public boolean hasCaptures() {
        return false;
    }

    /** Regular expressions are interned so that there is only one instance of
     * an equal regular expression. This is the canonical instance representing
     * that regular expression. The factory methods for regular expressions all
     * return canonical instances.
     */
    static RegExp canonicalize(RegExp r) {
        RegExp current = regExps.putIfAbsent(r,r);
        return current != null ? current : r;
    }

    /** The (canonical) Brzozowski derivative of this regular expression. */
    public RegExp derivative(int codepoint) {
        if (codepoint < ASCII) {
            if (asciiDerivatives == null) asciiDerivatives = new RegExp[ASCII];
            RegExp result = asciiDerivatives[codepoint];
            if (result == null) {
                result = computeDerivative(codepoint);
                asciiDerivatives[codepoint] = result;
            }
            return result;
        }
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

    /** The Brzozowski derivative of this regular expression, computed without
     * relying on the derivative cache to contain this derivative. It is provided
     * by subclasses. It always produces a canonical RE.
     */
    protected abstract RegExp computeDerivative(int codepoint);

    /** The derivative of this skeleton term as a list of (term, ops) pairs, none of
     *  whose terms is void. For an RE without captures, this is just the Brzozowski
     *  derivative with no ops, so sums without holes stay nested.
     */
    public List<DerivTerm> termDerivative(int codepoint) {
        if (!hasCaptures()) {
            RegExp d = derivative(codepoint);
            return d.isVoid() ? List.of() : List.of(new DerivTerm(d, Ops.NONE));
        }
        var result = termDerivatives.get(codepoint);
        if (result == null) {
            result = List.copyOf(computeTermDerivative(codepoint)); // immutable, so safe to share
            termDerivatives.put(codepoint, result);
        }
        return result;
    }

    /** The derivative of this term as a list of (term, ops) pairs, computed without
     *  using the cache. Only called when hasCaptures() is true, so only REs that
     *  can contain captures need to override it.
     */
    protected List<DerivTerm> computeTermDerivative(int codepoint) {
        throw new UnsupportedOperationException();
    }

    /** The ways this term can match the empty string, each given by the tag
     *  operations that complete its captures. Empty if not nullable. */
    public List<Ops> nullOps() {
        if (!hasCaptures()) return nullable() ? List.of(Ops.NONE) : List.of();
        List<Ops> result = nullOps;
        if (result == null) {
            result = List.copyOf(computeNullOps()); // immutable, so safe to publish racily
            nullOps = result;
        }
        return result;
    }

    /** Compute nullOps(); only called when hasCaptures() is true. */
    protected List<Ops> computeNullOps() {
        throw new UnsupportedOperationException();
    }

    /** Add a derivative term to a list, skipping void terms. */
    static void addTerm(List<DerivTerm> list, RegExp term, Ops ops) {
        if (!term.isVoid()) list.add(new DerivTerm(term, ops));
    }

    /** The list of distinct elements of a list, preserving order. */
    static <T> List<T> distinct(List<T> list) {
        return new ArrayList<>(new java.util.LinkedHashSet<>(list));
    }

    /** An equivalent RE in which every capture has an index, numbering the unnumbered
     *  captures in tree order starting from next[0], which is advanced. Throws
     *  IllegalArgumentException if a capture appears under a Kleene star.
     *  Default behavior: there are no captures, so return this.
     */
    protected RegExp numberCaptures(int[] next) {
        return this;
    }

    /** The largest capture index in this RE, or 0 if there are none. */
    protected int maxCaptureIndex() {
        return 0;
    }

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
