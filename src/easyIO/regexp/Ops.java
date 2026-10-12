package easyIO.regexp;

import java.util.Arrays;

/** Tag operations applied to a capture environment at the current input position:
 *  first all the starts (⊢k), then all the ends (⊣k). Ops never mention a position,
 *  so they depend only on the skeleton and can be cached with derivatives.
 *  Each capture fires at most once on any path, so a set of starts and a set of
 *  ends suffice.
 */
public final class Ops {
    private final int[] starts, ends; // sorted, no duplicates

    public static final Ops NONE = new Ops(new int[0], new int[0]);

    private Ops(int[] starts, int[] ends) {
        this.starts = starts;
        this.ends = ends;
    }

    /** ⊢k */
    static Ops start(int k) { return new Ops(new int[]{k}, new int[0]); }
    /** ⊣k */
    static Ops end(int k) { return new Ops(new int[0], new int[]{k}); }

    /** These ops followed by o. */
    Ops then(Ops o) {
        if (o == NONE) return this;
        if (this == NONE) return o;
        return new Ops(union(starts, o.starts), union(ends, o.ends));
    }

    private static int[] union(int[] a, int[] b) {
        return java.util.stream.IntStream.concat(Arrays.stream(a), Arrays.stream(b))
                .distinct().sorted().toArray();
    }

    /** Apply these ops at input position pos to the environment env. */
    Env apply(Env env, int pos) {
        if (this == NONE) return env;
        int[] s = env.starts.clone(), e = env.ends.clone();
        for (int k : starts) { s[k] = pos; e[k] = -1; }
        for (int k : ends) e[k] = pos;
        return new Env(s, e);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Ops p && Arrays.equals(starts, p.starts) && Arrays.equals(ends, p.ends);
    }
    @Override public int hashCode() {
        return Arrays.hashCode(starts) * 31 + Arrays.hashCode(ends);
    }
    @Override public String toString() {
        StringBuilder b = new StringBuilder("[");
        for (int k : starts) b.append("⊢").append(k);
        for (int k : ends) b.append("⊣").append(k);
        return b.append("]").toString();
    }

    /** A capture environment: a partial map from capture indices to spans of input
     *  positions. starts[k] == -1 means capture k is unmapped; otherwise ends[k] == -1
     *  means it is open, and [starts[k], ends[k]) is its span when complete.
     */
    static final class Env {
        // invariant: starts.length == ends.length
        private final int[] starts, ends;

        Env(int[] starts, int[] ends) {
            this.starts = starts;
            this.ends = ends;
        }

        int size() {
            return starts.length;
        }
        int start(int k) {
            return starts[k];
        }
        int end(int k) {
            return ends[k];
        }

        /** The environment with no captures mapped, for captures numbered 1..n. */
        static Env empty(int n) {
            int[] s = new int[n + 1], e = new int[n + 1];
            Arrays.fill(s, -1);
            Arrays.fill(e, -1);
            return new Env(s, e);
        }

        @Override public boolean equals(Object o) {
            return o instanceof Env v && Arrays.equals(starts, v.starts) && Arrays.equals(ends, v.ends);
        }
        @Override public int hashCode() {
            return Arrays.hashCode(starts) * 31 + Arrays.hashCode(ends);
        }
        @Override public String toString() {
            StringBuilder b = new StringBuilder("{");
            String sep = "";
            for (int k = 1; k < starts.length; k++) {
                if (starts[k] == -1) continue;
                b.append(sep).append(k).append("↦[").append(starts[k]).append(',')
                 .append(ends[k] == -1 ? "·" : Integer.toString(ends[k])).append(')');
                sep = ", ";
            }
            return b.append('}').toString();
        }
    }
}
