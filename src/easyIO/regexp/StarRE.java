package easyIO.regexp;

import static easyIO.regexp.Concat.concat;

/** Kleene Star RE (r*) */
public class StarRE extends RegExp {
    private final RegExp expr;
    private StarRE(RegExp r) {
        expr = r;
    }

    /** A canonical regular expression equivalent to r* */
    public static RegExp star(RegExp r) {
        // Kleene star is simplified using these rewrites:
        //   0* = 0
        //   1* = 1
        //   r** = r*
        if (r.isVoid()) return VoidRE.create();
        if (r instanceof StringRE s && s.nullable()) return s;
        if (r instanceof StarRE s) {
            return s;
        }
        return canonicalize(new StarRE(r));
    }
    /** A canonical regular expression equivalent to r+
     * (Unix notation for 1 or more consecutive matches)
     */
    public static RegExp oneOrMore(RegExp r) {
        return concat(r, star(r));
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        // Dx a* = (Dx a) a
        return concat(expr.derivative(codepoint), this);
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        expr.appendString(b, 3);
        b.append('*');
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StarRE r) {
            return expr.equals(r.expr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return expr.hashCode() * 17;
    }
}