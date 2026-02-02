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

    static class VoidResult extends DerivativeResult {
        public VoidResult() {
            super(instance);
        }
        @Override
        public Match transform(Match input, int codepoint) {
            return NoMatch.create(); // should never use anyway
        }
    }

    @Override
    protected DerivativeResult computeDerivative(int codepoint) {
        // Dx 0 = 0
        return new VoidResult();
    }

    @Override
    protected int numberCaptures(int index) {
        assert false;
        return index;
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
