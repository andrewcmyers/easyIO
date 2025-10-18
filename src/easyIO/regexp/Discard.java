package easyIO.regexp;

public class Discard implements Match {
    @Override
    public boolean success() {
        return true;
    }

    @Override
    public Match accept(int codepoint) {
        return this;
    }

    public String toString() {
        return "\"\"";
    }
}
