package easyIO.regexp;

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
        if (s.isEmpty()) return empty;
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
        // Avoid caching derivatives that are void.
        if (chars.isEmpty() || chars.codePointAt(0) != codepoint) {
            return VoidRE.create();
        }
        return super.derivative(codepoint);
    }

    protected RegExp computeDerivative(int codepoint) {
        // Dx 1 = 0
        // Dx x = 1
        // Dx y = 0  (y ≠ x)
        // so Dx xa = a
        //    Dx ya = 0
        if (!chars.isEmpty() && chars.codePointAt(0) == codepoint) {
            return string(chars.substring(Character.charCount(codepoint)));
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
        StringBuilder b = new StringBuilder();
        s.codePoints().forEach((int i) -> {
            switch (i) {
                case '*':
                case '(':
                case ')':
                case '|':
                case '\\':
                case '[':
                case ']':
                case '{':
                case '}':
                case '.':
                    b.append("\\");
                    b.append((char) i);
                    break;
                default:
                    appendEscaped(b, i);
            }
        });
        return b.toString();
    }

    /** Append a character, written as an escape sequence if it is a newline,
     *  carriage return, or tab. */
    static void appendEscaped(StringBuilder b, int c) {
        switch (c) {
            case '\n' -> b.append("\\n");
            case '\r' -> b.append("\\r");
            case '\t' -> b.append("\\t");
            default -> b.appendCodePoint(c);
        }
    }
}
