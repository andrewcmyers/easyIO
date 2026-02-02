package easyIO.regexp;

public class EmptyMatch implements Match {
    RegExp expression;

    public EmptyMatch(RegExp expr) {
        expression = expr;
    }

    @Override
    public Match scan(int codepoint) {
        var derivativeResult = expr.derivative(codepoint);
        var match = derivativeResult.transform(state.match(), codepoint);
        return new Matcher.State(derivativeResult.derivative, match);
    }
}