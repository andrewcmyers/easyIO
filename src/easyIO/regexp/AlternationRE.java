package easyIO.regexp;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static easyIO.regexp.CharacterClass.anyChar;
import static easyIO.regexp.StarRE.star;
import static easyIO.regexp.StringRE.string;

/**
 * A regular expression of the form r1|r2|r3|...|rn  (n >= 2)
 */
public class AlternationRE extends RegExp {
    private final RegExp[] exprs;
    private final boolean hasCaptures;

    /** An RE for r|s */
    private AlternationRE(RegExp ...rs) {
        exprs = rs;
        hasCaptures = Arrays.stream(exprs).anyMatch(RegExp::hasCaptures);
        assert Arrays.stream(exprs).noneMatch(RegExp::isVoid);
    }
    Stream<RegExp> exprs() {
        return Arrays.stream(exprs);
    }
    @Override public boolean isVoid() {
        return false; // alternatives are never void
    }
    @Override public boolean nullable() {
        return Arrays.stream(exprs).anyMatch(RegExp::nullable);
    }
    @Override public boolean hasCaptures() {
        return hasCaptures;
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        // Dx (a + b + ...) = (Dx a) + (Dx b) + ...
        return alt(Arrays.stream(exprs).map(r -> r.derivative(codepoint)).toArray(RegExp[]::new));
    }

    @Override
    protected List<DerivTerm> computeTermDerivative(int codepoint) {
        // Dx (a + b + ...) = (Dx a) + (Dx b) + ..., keeping the summands separate
        List<DerivTerm> result = new ArrayList<>();
        for (RegExp r : exprs) result.addAll(r.termDerivative(codepoint));
        return result;
    }

    @Override
    protected List<Ops> computeNullOps() {
        // ν(R+S) = νR + νS
        List<Ops> result = new ArrayList<>();
        for (RegExp r : exprs) result.addAll(r.nullOps());
        return distinct(result);
    }

    @Override
    protected RegExp numberCaptures(int[] next) {
        if (!hasCaptures) return this;
        return alt(Arrays.stream(exprs).map(r -> r.numberCaptures(next)).toArray(RegExp[]::new));
    }

    @Override
    protected void addCaptureIndices(List<Integer> indices) {
        if (!hasCaptures) return;
        for (RegExp r : exprs) r.addCaptureIndices(indices);
    }

    @Override
    protected int maxCaptureIndex() {
        return Arrays.stream(exprs).mapToInt(RegExp::maxCaptureIndex).max().orElse(0);
    }

    public static RegExp alt(RegExp ...exprs) {
        // alternation is simplified by:
        //  1) flattening nested alternatives
        //  2) dropping void alternatives
        //  3) dropping redundant alternatives
        //  4) absorbing everything else into Σ*, if present and there are no captures
        Set<RegExp> uniques = uniques(exprs);
        if (uniques.contains(everything) && uniques.stream().noneMatch(RegExp::hasCaptures))
            return everything;
        return fromUniques(uniques);
    }

    /** Like alt, but never absorbs alternatives into Σ*. Absorption is justified by
     *  language inclusion, not by an equation, so it would lose the capture state
     *  of the absorbed alternatives. */
    static RegExp altExact(RegExp ...exprs) {
        return fromUniques(uniques(exprs));
    }

    private static Set<RegExp> uniques(RegExp[] exprs) {
        return Arrays.stream(exprs).filter(r -> !r.isVoid())
                .flatMap(AlternationRE::flatten)
                .collect(Collectors.toSet());
    }

    private static RegExp fromUniques(Set<RegExp> uniques) {
        return switch (uniques.size()) {
            case 0 -> VoidRE.create();
            case 1 -> uniques.iterator().next();
            default -> canonicalize(new AlternationRE(uniques.toArray(RegExps)));
        };
    }

    /** A canonical RE for r? (Unix regex notation) */
    public static RegExp optional(RegExp r) {
        return alt(r, string(""));
    }

    public static final RegExp everything = star(anyChar());

    /** Recursively flatten alternatives in r.
     * E.g., (R+S)+(T+(U+V)) becomes R+S+T+U+V. */
    static Stream<RegExp> flatten(RegExp r) {
        if (r instanceof AlternationRE a) {
            return Arrays.stream(a.exprs).flatMap(AlternationRE::flatten);
        } else {
            return Stream.of(r);
        }
    }

    /** The top-level summands of r: its alternatives if it is a sum, none if it is
     *  void, and otherwise r itself. These are the positions of a matcher state. */
    static RegExp[] summands(RegExp r) {
        if (r instanceof AlternationRE a) return a.exprs;
        if (r.isVoid()) return RegExps;
        return new RegExp[]{r};
    }

    @Override
    public void appendString(StringBuilder sb, int precedence) {
        if (precedence > 1) sb.append("(");
        boolean first = true;
        for (RegExp r : exprs) {
            if (!first) sb.append('|');
            first = false;
            r.appendString(sb, 1);
        }
        if (precedence > 1) sb.append(")");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AlternationRE r) {
            if (exprs.length != r.exprs.length) return false;
            for (int i = 0; i < exprs.length; i++) {
                if (!exprs[i].equals(r.exprs[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override public int hashCode() {
        int result = 0;
        for (int i = 0; i < exprs.length; i++) {
            result *= 101;
            result += exprs[i].hashCode();
        }
        return result;
    }
}
