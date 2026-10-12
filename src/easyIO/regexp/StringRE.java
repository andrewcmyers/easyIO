package easyIO.regexp;

/**
 * A string of characters, possibly empty
 */
public class StringRE extends RegExp {
    private static final RegExp empty = new StringRE("");
    private final String chars;
    private Object sameMatch;

    private StringRE(String s) {
        chars = s;
    }

    public static RegExp string(String s) {
        return canonicalize(new StringRE(s));
    }

    public static RegExp empty() {
        return empty;
    }

    public boolean nullable() {
        return chars.isEmpty();
    }

    static class Unchanged extends DerivativeResult {
        Unchanged(RegExp derivative) {
            super(derivative);
        }

        @Override
        public Match transform(Match input, int codepoint) {
            return input;
        }
    }

    @Override
    public DerivativeResult derivative(int codepoint) {
        if (chars.isEmpty() || chars.charAt(0) != codepoint) {
            return new Unchanged(VoidRE.create());
        }
        return super.derivative(codepoint);
    }

    protected DerivativeResult computeDerivative(int codepoint) {
        // Dx 1 = 0
        // Dx x = 1
        // Dx y = 0  (y ≠ x)
        // so Dx xa = a
        //    Dx ya = 0
        if (!chars.isEmpty() && chars.charAt(0) == codepoint) {
            return new DerivativeResult(string(chars.substring(1)), sameMatch);
        }
        return new DerivativeResult(VoidRE.create(), sameMatch);
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        if (precedence > 2 && chars.length() > 1) b.append('(');
        b.append(quote(chars));
        if (precedence > 2 && chars.length() > 1) b.append(')');
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StringRE r) {
            return chars.equals(r.chars);
        }
        return false;
    }

    @Override public int hashCode() {
        return chars.hashCode();
    }

    public static String quote(String s) {
        StringBuffer b = new StringBuffer();
        s.codePoints().forEach((int i) -> {
            switch (i) {
                case '*':
                case '(':
                case ')':
                case '|':
                case '\\':
                case '[':
                case ']':
                    b.append("\\");
                    b.append((char) i);
                    break;
                default:
                    b.appendCodePoint(i);
            }
        });
        return b.toString();
    }

//    public class StringMatch extends Match {
//        StringBuilder builder = new StringBuilder();
//        Match parent;
//
//        StringMatch(Match parent) {
//            this.parent = parent;
//        }
//
//        @Override
//        public boolean success() {
//            return true;
//        }
//
//        @Override
//        public Match accept(int codepoint) {
//            builder.appendCodePoint(codepoint);
//            if (nullable()) {
//                return parent;
//            } else {
//                return this;
//            }
//        }
//    }
}
