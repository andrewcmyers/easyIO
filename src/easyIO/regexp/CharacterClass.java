package easyIO.regexp;

import static easyIO.regexp.StringRE.empty;
import static easyIO.regexp.StringRE.quote;

public class CharacterClass extends RegExp {
    private int start, end;
    boolean negated;
    public CharacterClass(int start, int end, boolean negated) {
        this.start = start;
        this.end = end;
        this.negated = negated;
    }
    public static RegExp range(int start, int end) {
        return canonicalize(new CharacterClass(start, end, false));
    }
    public static RegExp excludeRange(int start, int end) {
        return canonicalize(new CharacterClass(start, end, true));
    }
    public static RegExp anyChar() {
        return canonicalize(new CharacterClass(1, 0, true));
    }

    @Override
    public RegExp derivative(int codepoint) {
        if ((codepoint >= start && codepoint <= end) != negated
            || (start > end) == negated) {
            return empty();
        } else {
            return VoidRE.create();
        }
    }

    @Override
    protected RegExp computeDerivative(int codepoint) {
        return derivative(codepoint);
    }

    @Override
    public void appendString(StringBuilder b, int precedence) {
        b.append('[');
        if (negated) b.append('^');
        if (start <= end) {
            b.append(quote(Character.toString(start)));
            b.append('-');
            b.append(quote(Character.toString(end)));
        }
        b.append(']');
    }

    @Override public boolean equals(Object o) {
        if (o instanceof CharacterClass cc) {
            return (start == cc.start && end == cc.end && negated == cc.negated);
        }
        return false;
    }
    @Override public int hashCode() {
        return start*173 + end*97 + (negated ? 1 : 0);
    }
}