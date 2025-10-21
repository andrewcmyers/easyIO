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
    private boolean isVoid, computedVoid = false;

    /** An RE for r|s */
    private AlternationRE(RegExp ...rs) {
        exprs = rs;
        assert !Arrays.stream(exprs).anyMatch(RegExp::isVoid);
    }
    @Override public boolean isVoid() {
        if (computedVoid) return isVoid;
        isVoid = Arrays.stream(exprs).allMatch(RegExp::isVoid);
        computedVoid = true;
        return isVoid;
    }
    @Override public boolean nullable() {
        return Arrays.stream(exprs).anyMatch(RegExp::nullable);
    }


    @Override
    protected RegExp computeDerivative(int codepoint) {
        // Dx (a + b) = (Dx a) + (Dx b)
        RegExp[] derivatives = Arrays.stream(exprs)
                .map(r -> r.derivative(codepoint))
                .toList()
                .toArray(RegExps);
        return alt(derivatives);
    }

    public static RegExp alt(RegExp ...exprs) {
        // alternation is simplified by:
        //  1) flattening nested alternatives
        //  2) dropping void alternatives
        //  3) dropping redundant alternatives
        //  4) sorting alternatives
        Set<RegExp> uniques =
                Arrays.stream(exprs).filter(r -> !r.isVoid())
                        .flatMap(AlternationRE::flatten)
                        .collect(Collectors.toSet());
        if (uniques.contains(everything)) return everything;
        RegExp[] uniques_a = uniques.toArray(RegExps);
        Arrays.sort(uniques_a, Comparator.comparingInt(RegExp::hashCode));
        return switch (uniques_a.length) {
            case 0 -> VoidRE.create();
            case 1 -> uniques_a[0];
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

    @Override
    public void appendString(StringBuilder sb, int precedence) {
        if (precedence > 1) sb.append("(");
        boolean first = true;
        for (RegExp r : exprs) {
            if (!first) sb.append('|');
            first = false;
            assert !r.isVoid();
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
