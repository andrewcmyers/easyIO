package easyIO.regexp;

public class EmptyMatch implements Match {
    @Override
    public Match accept(int codepoint) {
        return this; // discard
    }

    public String toString() {
        return "\"\"";
    }
}
