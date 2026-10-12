package easyIO.regexp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.StringRE.empty;

/** A regular expression of the form r1r2r3...
 */
public class Concat extends RegExp {
    private final RegExp[] exprs;
    private final boolean hasCaptures;

    private Concat(RegExp... exprs) {
        this.exprs = exprs.clone();
        hasCaptures = Arrays.stream(exprs).anyMatch(RegExp::hasCaptures);
    }

    /**
     * A canonical RE equivalent to r0r1r2...
     **/
    public static RegExp concat(RegExp... rs) {
        for (RegExp r : rs) {
            if (r.isVoid()) return VoidRE.create();
        }
        RegExp[] exprs = Arrays.stream(rs)
                .flatMap(Concat::flatten)
                .filter(r -> !r.equals(empty()))
                .toList()
                .toArray(RegExps);
        return switch (exprs.length) {
            case 0 -> empty();
            case 1 -> exprs[0];
            default -> canonicalize(new Concat(exprs));
        };
    }

    static Stream<RegExp> flatten(RegExp r) {
        if (r instanceof Concat c) {
            return Arrays.stream(c.exprs).flatMap(Concat::flatten);
        } else {
            return Stream.of(r);
        }
    }

    public boolean nullable() {
        return Arrays.stream(exprs).allMatch(RegExp::nullable);
    }

    @Override
    public boolean hasCaptures() {
        return hasCaptures;
    }

    /** The concatenation of all but the first element. */
    private RegExp rest() {
        return concat(Arrays.copyOfRange(exprs, 1, exprs.length));
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        // Dx (ab) = (Dx a) b + (n a) (Dx b)
        assert exprs.length > 1;
        var result = VoidRE.create();
        for (int i = 0; i < exprs.length; i++) {
            RegExp[] args = new RegExp[exprs.length - i];
            args[0] = exprs[i].derivative(codepoint);
            for (int j = 1; j < args.length; j++) {
                args[j] = exprs[i + j];
            }
            if (args.length == 1)
                result = alt(result, args[0]);
            else
                result = alt(result, concat(args));
            if (!exprs[i].nullable()) break;
        }
        return result;
    }

    @Override
    protected List<DerivTerm> computeTermDerivative(int codepoint) {
        // d(RS) = (dR)S + (νR)(dS), keeping each summand separate so that
        // sums containing holes are distributed over S.
        RegExp head = exprs[0], rest = rest();
        List<DerivTerm> result = new ArrayList<>();
        for (DerivTerm d : head.termDerivative(codepoint)) {
            addTerm(result, concat(d.term(), rest), d.ops());
        }
        List<Ops> headNull = head.nullOps();
        if (!headNull.isEmpty()) {
            for (DerivTerm d : rest.termDerivative(codepoint)) {
                for (Ops n : headNull) {
                    addTerm(result, d.term(), n.then(d.ops()));
                }
            }
        }
        return result;
    }

    @Override
    protected List<Ops> computeNullOps() {
        // ν(RS) = (νR)(νS)
        List<Ops> result = List.of(Ops.NONE);
        for (RegExp r : exprs) {
            List<Ops> n = r.nullOps();
            List<Ops> next = new ArrayList<>();
            for (Ops a : result) {
                for (Ops b : n) next.add(a.then(b));
            }
            result = distinct(next);
            if (result.isEmpty()) break;
        }
        return result;
    }

    @Override
    protected RegExp numberCaptures(int[] next) {
        if (!hasCaptures) return this;
        RegExp[] numbered = new RegExp[exprs.length];
        for (int i = 0; i < exprs.length; i++) {
            numbered[i] = exprs[i].numberCaptures(next);
        }
        return concat(numbered);
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

    @Override
    public void appendString(StringBuilder b, int precedence) {
        if (precedence > 2) b.append("(");
        for (var r : exprs) {
            r.appendString(b, 2);
        }
        if (precedence > 2) b.append(")");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Concat r) {
            if (exprs.length != r.exprs.length) return false;
            for (int i = 0; i < exprs.length; i++) {
                if (exprs[i] != r.exprs[i]) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (var r : exprs) {
            result *= 173;
            result ^= r.hashCode();
        }
        return result;
    }
}
