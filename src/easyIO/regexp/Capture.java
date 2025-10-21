package easyIO.regexp;

public class Capture extends RegExp {
    RegExp pattern;
    private Capture(RegExp e) {
        pattern = e;
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
    protected RegExp computeDerivative(int codepoint) {
        RegExp next = pattern.derivative(codepoint);
        if (next.isVoid()) return VoidRE.create();
        return new Capture(next);
    }

    public Matcher.State scan(int codepoint) {
        var derivative = derivative(codepoint);
        return new Matcher.State(derivative);
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append("((");
        pattern.appendString(b, 0);
        b.append("))");
    }
}