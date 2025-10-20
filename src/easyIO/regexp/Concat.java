package easyIO.regexp;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.StringRE.empty;
import static easyIO.regexp.StringRE.string;

/** A regular expression of the form r1r2r3...
 */
public class Concat extends RegExp {
    private final RegExp[] exprs;

    private Concat(RegExp... exprs) {
        this.exprs = exprs.clone();
    }

    /**
     * A canonical RE equivalent to r0r1r2...
     **/
    public static RegExp concat(RegExp... rs) {
        int n = rs.length;
        for (RegExp r : rs) {
            if (r.isVoid()) return VoidRE.create();
        }
        RegExp[] exprs = Arrays.stream(rs)
                .flatMap(Concat::flatten)
                .filter(r -> !r.equals(empty()))
                .toList()
                .toArray(new RegExp[0]);
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