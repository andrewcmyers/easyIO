package easyIO.regexp;

/// An active capture ⟨m|R⟧, with skeleton state ⟨R⟧.
public class ActiveCapture extends RegExp {
    final RegExp pattern;
    final int captureIndex; // index of this capture inside its RE

    ActiveCapture(RegExp pattern, int captureIndex) {
        this.pattern = pattern;
        this.captureIndex = captureIndex;
    }

    @Override
    protected DerivativeResult computeDerivative(int codepoint) {
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {

    }
}
