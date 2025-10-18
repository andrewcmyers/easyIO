package easyIO.regexp;

public class VoidRE extends RegExp {
    static private final RegExp theRE = new VoidRE();
    @Override public boolean isVoid() {
        return true;
    }
    @Override public boolean nullable() {
        return false;
    }

    @Override
    protected Match updateMatch(Match match, int codepoint) {
        return match;
    }

    public static RegExp create() {
        return theRE;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        // Dx 0 = 0
        return this;
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append("[^]");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RegExp r) {
            return r.isVoid();
        }
        return false;
    }
}
