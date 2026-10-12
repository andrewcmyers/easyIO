package easyIO.regexp;

public class VoidRE extends RegExp {
    static private final RegExp instance = new VoidRE();
    @Override public boolean isVoid() {
        return true;
    }
    @Override public boolean nullable() {
        return false;
    }

    public static RegExp create() {
        return instance;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        // Dx 0 = 0
        return instance;
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append("[]");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RegExp r) {
            return r.isVoid();
        }
        return false;
    }
}
