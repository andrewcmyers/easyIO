package easyIO.regexp;

import java.util.ArrayList;
import java.util.List;

/// An active capture ⟨m|R⟧k, with skeleton state ⟨R⟧k. The captured string m is
/// held in the capture state on the side.
public class ActiveCapture extends RegExp {
    final RegExp pattern;
    final int captureIndex; // index of this capture inside its RE

    private ActiveCapture(RegExp pattern, int captureIndex) {
        this.pattern = pattern;
        this.captureIndex = captureIndex;
    }

    /** A canonical RE for the open capture ⟨R⟧k. ⟨∅⟧k = ∅. */
    static RegExp open(RegExp pattern, int captureIndex) {
        if (pattern.isVoid()) return VoidRE.create();
        return canonicalize(new ActiveCapture(pattern, captureIndex));
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
    public boolean hasCaptures() {
        return true;
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        return open(pattern.derivative(codepoint), captureIndex);
    }

    @Override
    protected List<DerivTerm> computeTermDerivative(int codepoint) {
        // d⟨R⟧k = ⟨dR⟧k, with no ops: the open span grows with the input position
        List<DerivTerm> result = new ArrayList<>();
        for (DerivTerm d : pattern.termDerivative(codepoint)) {
            addTerm(result, open(d.term(), captureIndex), d.ops());
        }
        return result;
    }

    @Override
    protected List<Ops> computeNullOps() {
        // ν⟨R⟧k = ⟨⟩k νR, where the capture was already started
        Ops e = Ops.end(captureIndex);
        return pattern.nullOps().stream().map(o -> o.then(e)).toList();
    }

    @Override
    protected void addCaptureIndices(List<Integer> indices) {
        indices.add(captureIndex);
        pattern.addCaptureIndices(indices);
    }

    @Override
    protected int maxCaptureIndex() {
        return Math.max(captureIndex, pattern.maxCaptureIndex());
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append("⟨");
        pattern.appendString(b, 0);
        b.append("⟧").append(captureIndex);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ActiveCapture c && c.captureIndex == captureIndex && c.pattern.equals(pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode() * 181 + captureIndex;
    }
}
