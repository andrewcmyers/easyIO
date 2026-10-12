package easyIO.regexp;

import java.util.ArrayList;
import java.util.List;

/// An unstarted capture pattern ⟦R⟧k
public class Capture extends RegExp {
    final RegExp pattern;
    final int captureIndex; // -1 (not yet numbered) or the index of this capture inside its RE

    private Capture(RegExp e, int captureIndex) {
        pattern = e;
        this.captureIndex = captureIndex;
    }

    /** A capture of e, to be numbered when a Matcher is created. Unnumbered captures
     *  are not interned, so distinct calls produce distinct captures. */
    public static RegExp capture(RegExp e) {
        if (e.isVoid()) return VoidRE.create();
        return new Capture(e, -1);
    }

    /** A capture of e with index k (k ≥ 1). */
    public static RegExp capture(RegExp e, int k) {
        if (e.isVoid()) return VoidRE.create();
        return canonicalize(new Capture(e, k));
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
        // Used only for recognition; the derivative opens the capture.
        return ActiveCapture.open(pattern.derivative(codepoint), captureIndex);
    }

    @Override
    protected List<DerivTerm> computeTermDerivative(int codepoint) {
        // d⟦R⟧k = ⟨dR⟧k with ⊢k
        List<DerivTerm> result = new ArrayList<>();
        for (DerivTerm d : pattern.termDerivative(codepoint)) {
            addTerm(result, ActiveCapture.open(d.term(), captureIndex),
                    Ops.start(captureIndex).then(d.ops()));
        }
        return result;
    }

    @Override
    protected List<Ops> computeNullOps() {
        // ν⟦R⟧k = ⟨⟩k νR
        Ops se = Ops.start(captureIndex).then(Ops.end(captureIndex));
        return pattern.nullOps().stream().map(se::then).toList();
    }

    @Override
    protected RegExp numberCaptures(int[] next) {
        RegExp p = pattern.numberCaptures(next);
        if (captureIndex != -1 && p == pattern) return this;
        int k = captureIndex != -1 ? captureIndex : next[0]++;
        return capture(p, k);
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
        b.append("{");
        pattern.appendString(b, 0);
        b.append("}");
    }

    @Override
    public boolean equals(Object o) {
        if (captureIndex == -1) return this == o;
        return o instanceof Capture c && c.captureIndex == captureIndex && c.pattern.equals(pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode() * 179 + captureIndex;
    }
}
