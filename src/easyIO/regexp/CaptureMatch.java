package easyIO.regexp;

public class CaptureMatch implements Match {
    private StringBuilder sb;
    private int captureIndex;

    public CaptureMatch(int captureIndex) {
        sb = new StringBuilder();
    }

    @Override
    public Match accept(int codepoint) {
        sb.appendCodePoint(codepoint);
        return this; // can we get away with reusing the same object and stringbuilder?
    }
}