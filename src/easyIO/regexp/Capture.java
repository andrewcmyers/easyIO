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
    protected Match updateMatch(Match match, int codepoint) {
        return match;
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        RegExp next = pattern.derivative(codepoint);
        if (next.isVoid()) return VoidRE.create();
        return new Capture(next);
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append('(');
        pattern.appendString(b, 0);
        b.append(')');
    }

//    private static class Captured extends Capture {
//        StringBuilder saved;
//        public Captured(RegExp next) {
//            pattern = next;
//            saved = new StringBuilder();
//        }
//
//        @Override
//        public boolean nullable() {
//            return pattern.nullable();
//        }
//
//        @Override
//        public boolean isVoid() {
//            return pattern.isVoid();
//        }
//
//        @Override
//        protected Match updateMatch(Match match, int codepoint) {
//            saved.appendCodePoint(codepoint);
//            return match.accept(codepoint);
//        }
//
//        @Override
//        protected RegExp computeDerivative(int codepoint) {
//            RegExp next = pattern.derivative(codepoint);
//            saved.appendCodePoint(codepoint);
//            return new Captured(next);
//        }
//
//        @Override
//        public void appendString(StringBuilder b, int precedence) {
//            b.append('(');
//            pattern.appendString(b, 0);
//            b.append(')');
//        }
//    }
}