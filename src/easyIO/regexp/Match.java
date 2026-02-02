package easyIO.regexp;

/// The state of a regular expression matcher
public interface Match {
    RegExp regexp();
    Match scan(int codepoint);
    default boolean isVoid() { return false; }

    Match finish();
}