package easyIO.regexp;

/** An in-progress result of matching a regular expression to a string of codepoints */
public interface Match {
    /** Whether this match succeeded */
    boolean success();
    /** Add a codepoint to this match */
    Match accept(int codepoint);
}
