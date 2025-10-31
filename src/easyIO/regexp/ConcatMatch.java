package easyIO.regexp;

import java.util.HashMap;
import java.util.Map;

/** An in-progress result of matching a concatenated regular expression to a string of codepoints */
public class ConcatMatch implements Match {
    /** Whether this match succeeded */
    Map<Integer, Match> captures;
    public ConcatMatch() {
        captures = new HashMap<>();
    }
    public ConcatMatch(ConcatMatch toCopy) {
        captures = new HashMap<>(toCopy.captures);
    }

    @Override
    public Match accept(int codepoint) {
        return null;
    }
}