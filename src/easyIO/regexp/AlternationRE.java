package easyIO.regexp;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static easyIO.StdIO.println;
import static easyIO.regexp.StringRE.string;

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
    protected Match updateMatch(Match match, int codepoint) {
        return new AlternationMatch(match, codepoint);
    }

    static private RegExp[] RegExps = {};

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
        //  3) combining equal alternatives
        Set<RegExp> uniques =
                Arrays.stream(exprs).filter(r -> !r.isVoid())
                        .flatMap(AlternationRE::flatten)
                        .collect(Collectors.toSet());
        RegExp[] uniques_a = uniques.toArray(RegExps);
        Arrays.sort(uniques_a, Comparator.comparingInt(RegExp::hashCode));
        return switch (uniques_a.length) {
            case 0 -> VoidRE.create();
            case 1 -> uniques_a[0];
            default -> RegExp.canonicalize(new AlternationRE(uniques.toArray(RegExps)));
        };
    }
    public static RegExp optional(RegExp r) {
        return alt(r, string(""));
    }
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

    class AlternationMatch implements Match {
        Match[] alternatives;
        Match parent;

        public AlternationMatch(Match match, int codepoint) {
            parent = match;
            alternatives = new Match[0];
            alternatives = Arrays.stream(exprs).map(r -> r.updateMatch(match, codepoint))
                    .toList().toArray(alternatives);
        }

        @Override
        public boolean success() {
            return false;
        }

        @Override
        public Match accept(int codepoint) {
            for (Match m : alternatives) {
                m.accept(codepoint);
            }
            return this;
        }
    }
}
