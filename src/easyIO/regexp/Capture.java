package easyIO.regexp;

/// An unstarted capture pattern ⟦R⟧k
public class Capture extends RegExp {
    final RegExp pattern;
    int captureIndex; // -1 or the index of this capture inside its RE
    private Capture(RegExp e) {
        pattern = e;
        captureIndex = -1;
    }
    private Capture(RegExp e, int captureIndex) {
        pattern = e;
        this.captureIndex = captureIndex;
    }

    public static RegExp capture(RegExp e) {
        return new Capture(e);
    }

    @Override
    public boolean nullable() {
        return pattern.nullable();
    }
    @Override
    public boolean isVoid() {
        return pattern.isVoid();
    }

    @Override
    public DerivativeResult computeDerivative(int codepoint) {
        var nextExpr = pattern.derivative(codepoint);
        // discarding nextExpr.transformer here...
        var nextRE = new Capture(nextExpr.derivative(), captureIndex);
        return new DerivativeResult(new ActiveCapture(nextRE, captureIndex))
        {
            @Override
            public Match transform(Match input, int codepoint) {
                return null; // TODO
            }
        };
    }

    @Override
    protected int numberCaptures(int index) {
        if (index == -1)
            throw new IllegalArgumentException("a capture cannot occur at this position inside a regular expression");
        captureIndex = index;
        return index+1;
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append("⸨");
          pattern.appendString(b, 0);
        b.append("⸩");
    }

    public int hashCode() {
        return pattern.hashCode() * 179;
    }
}