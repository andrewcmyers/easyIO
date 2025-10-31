package easyIO.regexp;

public class AltMatch implements Match {
    AlternationRE expr;
    Match[] matches;
    public AltMatch(AlternationRE altExpr) {
        expr = altExpr;
        matches = new ConcatMatch[expr.exprs().toList().size()];
        for (int i = 0; i < matches.length; i++) {
            matches[i] = new EmptyMatch();
        }
    }

    @Override
    public ConcatMatch accept(int codepoint) {
        ConcatMatch[] updatedMatches = new ConcatMatch[matches.length];
        for (int i = 0; i < matches.length; i++) {
            updatedMatches[i] = matches[i].accept(codepoint);
        }
        // XXX
        return new AltMatch(expr);
    }
}
