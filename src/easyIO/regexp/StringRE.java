package easyIO.regexp;

import static easyIO.StdIO.println;

/**
 * A string of characters, possibly empty
 */
public class StringRE extends RegExp {
    private static final RegExp theRE = new StringRE("");
    private final String string;

    private StringRE(String s) {
        string = s;
    }

    public static RegExp string(String s) {
        return RegExp.canonicalize(new StringRE(s));
    }

    public static RegExp empty() {
        return theRE;
    }

    public boolean nullable() {
        return string.isEmpty();
    }

    @Override
    protected Match updateMatch(Match match, int codepoint) {
        return match;
    }

    protected RegExp computeDerivative(int codepoint) {
        // Dx 1 = 0
        // Dx x = 1
        // Dx y = 0  (y ≠ x)
        // so Dx xa = a
        //    Dx ya = 0
        if (!string.isEmpty() && string.charAt(0) == codepoint) {
            return new StringRE(string.substring(1));
        }
        return VoidRE.create();
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append(quote(string));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StringRE r) {
            return string.equals(r.string);
        }
        return false;
    }

    @Override public int hashCode() {
        return string.hashCode();
    }

    private String quote(String s) {
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

    public class StringMatch implements Match {
        StringBuilder builder = new StringBuilder();
        Match parent;

        StringMatch(Match parent) {
            this.parent = parent;
        }

        @Override
        public boolean success() {
            return true;
        }

        @Override
        public Match accept(int codepoint) {
            builder.appendCodePoint(codepoint);
            if (nullable()) {
                return parent;
            } else {
                return this;
            }
        }
    }
}
