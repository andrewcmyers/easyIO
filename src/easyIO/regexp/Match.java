package easyIO.regexp;

/** One way a regular expression matched some input: the matched text and the
 *  span captured by each capture group. Group 0 is the whole match; groups
 *  1..groupCount() are the captures, numbered as in the regular expression.
 *  Positions are char offsets into the matched text.
 */
public final class Match {
    private final String text;
    private final Ops.Env env;

    Match(String text, Ops.Env env) {
        this.text = text;
        this.env = env;
    }

    /** The number of capture groups (not counting group 0). */
    public int groupCount() {
        return env.size() - 1;
    }

    /** Whether capture k matched something (possibly empty) in this match. */
    public boolean isSet(int k) {
        return k == 0 || env.start(k) != -1;
    }

    /** The start of capture k, or -1 if it did not participate in this match. */
    public int start(int k) {
        return k == 0 ? 0 : env.start(k);
    }

    /** The end of capture k, or -1 if it did not participate in this match. */
    public int end(int k) {
        return k == 0 ? text.length() : env.end(k);
    }

    /** The text captured by capture k, or null if it did not participate in this match. */
    public String group(int k) {
        if (!isSet(k)) return null;
        return text.substring(start(k), end(k));
    }

    @Override public boolean equals(Object o) {
        return o instanceof Match m && text.equals(m.text) && env.equals(m.env);
    }
    @Override public int hashCode() {
        return text.hashCode() * 31 + env.hashCode();
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder();
        String sep = "";
        for (int k = 1; k <= groupCount(); k++) {
            if (!isSet(k)) continue;
            b.append(sep).append('⟨').append(group(k)).append('⟩').append(k);
            sep = " ";
        }
        return b.toString();
    }
}
