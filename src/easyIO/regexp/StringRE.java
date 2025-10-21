package easyIO.regexp;

import static easyIO.StdIO.println;

/**
 * A string of characters, possibly empty
 */
public class StringRE extends RegExp {
    private static final RegExp empty = new StringRE("");
    private final String chars;

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

    @Override
    public RegExp derivative(int codepoint) {
        if (chars.isEmpty() || chars.charAt(0) != codepoint) return VoidRE.create();
        return super.derivative(codepoint);
    }

    protected RegExp computeDerivative(int codepoint) {
        // Dx 1 = 0
        // Dx x = 1
        // Dx y = 0  (y ≠ x)
        // so Dx xa = a
        //    Dx ya = 0
        if (!chars.isEmpty() && chars.charAt(0) == codepoint) {
            return string(chars.substring(1));
        }
        return VoidRE.create();
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
