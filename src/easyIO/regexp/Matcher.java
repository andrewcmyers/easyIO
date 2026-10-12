package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;
import easyIO.regexp.Ops.Env;
import easyIO.regexp.RegExp.DerivTerm;

import java.io.StringReader;
import java.util.*;

import static easyIO.StdIO.println;
import static easyIO.regexp.AlternationRE.summands;

/** A matcher for a regular expression with captures. The matcher state is a skeleton
 *  RE whose positions are its top-level summands, plus a capture state: for each
 *  position, the set of capture environments of that summand. See notes2.txt.
 */
public class Matcher {
    /// A potentially invalid state that the matcher went through at input position `inputPosition` while searching.
    private record IndexedState(int inputPosition, RegExp r) {}

    /** The cached transition of a matcher state on one input symbol: the target state,
     *  and for each target position j, the source positions sources[j] that feed it and
     *  the ops ops[j] applied along each of those edges. */
    static final class Transition {
        final RegExp target;
        final int[][] sources;
        final Ops[][] ops;

        Transition(RegExp target, int[][] sources, Ops[][] ops) {
            this.target = target;
            this.sources = sources;
            this.ops = ops;
        }
    }

    private static final boolean DEBUG = false;
    private final RegExp expr;
    private final int numCaptures;

    private Set<IndexedState> hopelessStates = null;

    /** Create a matcher for r. Captures in r that do not have an index are numbered
     *  in tree order, after any captures that do.
     *  Throws IllegalArgumentException if a capture occurs inside a Kleene star. */
    public Matcher(RegExp r) {
        expr = r.numberCaptures(new int[]{Math.max(0, r.maxCaptureIndex()) + 1});
        numCaptures = expr.maxCaptureIndex();
    }
    /** Create a matcher based on a regular expression to be parsed.
     * Requires: the regular expression must be valid. */
    public Matcher(String pat) {
        this(parseOrFail(pat));
    }

    private static RegExp parseOrFail(String pat) {
        try {
            return Parser.parse(pat);
        } catch (Parser.SyntaxError e) {
            throw new IllegalArgumentException("Invalid regular expression: " + pat);
        }
    }

    public static class FailedMatch extends Exception {
        static FailedMatch exception = new FailedMatch();
    }

    /** One run of the matcher over some input: the current skeleton state and the
     *  capture state aligned with its positions. The capture state is null when the
     *  regular expression has no captures, in which case the state is just the
     *  Brzozowski derivative. */
    private final class Run {
        RegExp state = expr;
        List<Set<Env>> captures;
        final StringBuilder text = new StringBuilder();

        Run() {
            if (expr.hasCaptures()) {
                captures = new ArrayList<>();
                for (RegExp ignored : summands(expr)) {
                    captures.add(Set.of(Env.empty(numCaptures)));
                }
            }
        }

        void step(int codepoint) {
            int pos = text.length();
            if (captures == null) {
                state = state.derivative(codepoint);
            } else {
                Transition t = transition(state, codepoint);
                List<Set<Env>> next = new ArrayList<>(t.sources.length);
                for (int j = 0; j < t.sources.length; j++) {
                    Set<Env> envs = new LinkedHashSet<>();
                    for (int e = 0; e < t.sources[j].length; e++) {
                        Ops ops = t.ops[j][e];
                        for (Env env : captures.get(t.sources[j][e])) {
                            envs.add(ops.apply(env, pos)); // the union is R+R=R
                        }
                    }
                    next.add(envs);
                }
                state = t.target;
                captures = next;
            }
            text.appendCodePoint(codepoint);
            if (DEBUG) println(pos + ". State is now " + state + ", captures " + captures);
        }

        /** The matches if the input ends here, in no particular order. Empty if the
         *  input seen so far does not match. */
        List<Match> results() {
            if (!state.nullable()) return List.of();
            String s = text.toString();
            if (captures == null) return List.of(new Match(s, Env.empty(0)));
            Ops[][] accept = acceptOps(state);
            Set<Env> envs = new LinkedHashSet<>();
            for (int i = 0; i < accept.length; i++) {
                for (Ops ops : accept[i]) {
                    for (Env env : captures.get(i)) envs.add(ops.apply(env, s.length()));
                }
            }
            return envs.stream().map(env -> new Match(s, env)).toList();
        }
    }

    /** The (cached) transition of matcher state `state` on `codepoint`. */
    static Transition transition(RegExp state, int codepoint) {
        if (state.transitions == null) state.transitions = new HashMap<>();
        Transition t = state.transitions.get(codepoint);
        if (t == null) {
            t = computeTransition(state, codepoint);
            state.transitions.put(codepoint, t);
        }
        return t;
    }

    private record Edge(int source, RegExp term, Ops ops) {}
    private record SourceOps(int source, Ops ops) {}

    private static Transition computeTransition(RegExp state, int codepoint) {
        // Take the derivative of each summand, then let altExact canonicalize the
        // target. Each resulting term is found among the target's summands by lookup;
        // terms that land on the same position have their environments unioned.
        RegExp[] src = summands(state);
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < src.length; i++) {
            for (DerivTerm d : src[i].termDerivative(codepoint)) {
                // A capture-free term may itself be a sum; its pieces share its environments.
                final int source = i;
                AlternationRE.flatten(d.term()).forEach(t -> edges.add(new Edge(source, t, d.ops())));
            }
        }
        RegExp target = AlternationRE.altExact(edges.stream().map(Edge::term).toArray(RegExp[]::new));
        RegExp[] tgt = summands(target);
        Map<RegExp, Integer> position = new HashMap<>();
        for (int j = 0; j < tgt.length; j++) position.put(tgt[j], j);
        List<Set<SourceOps>> into = new ArrayList<>();
        for (RegExp ignored : tgt) into.add(new LinkedHashSet<>());
        for (Edge e : edges) {
            Integer j = position.get(e.term());
            assert j != null : "term " + e.term() + " missing from " + target;
            into.get(j).add(new SourceOps(e.source(), e.ops()));
        }
        int[][] sources = new int[tgt.length][];
        Ops[][] ops = new Ops[tgt.length][];
        for (int j = 0; j < tgt.length; j++) {
            sources[j] = into.get(j).stream().mapToInt(SourceOps::source).toArray();
            ops[j] = into.get(j).stream().map(SourceOps::ops).toArray(Ops[]::new);
        }
        return new Transition(target, sources, ops);
    }

    /** The (cached) accepting ops of each position of matcher state `state`. */
    static Ops[][] acceptOps(RegExp state) {
        if (state.acceptOps == null) {
            state.acceptOps = Arrays.stream(summands(state))
                    .map(r -> r.nullOps().toArray(new Ops[0]))
                    .toArray(Ops[][]::new);
        }
        return state.acceptOps;
    }

    /** All the ways the regular expression matches the entire input s. */
    public List<Match> match(String s) throws FailedMatch {
        return match(new BacktrackScanner(new BacktrackScanner.ReaderSource(new StringReader(s), s)));
    }

    /** All the ways the regular expression matches the entire input, in no
     *  particular order. Throws FailedMatch if there are none. */
    public List<Match> match(BacktrackScanner s) throws FailedMatch {
        Run run = new Run();
        while (s.hasNext()) {
            int ch;
            try {
                ch = s.nextCodePoint();
            } catch (EOF exc) {
                break;
            }
            run.step(ch);
            if (run.state.isVoid()) {
                throw FailedMatch.exception;
            }
        }
        List<Match> result = run.results();
        if (result.isEmpty()) throw FailedMatch.exception;
        return result;
    }

    /** Find the first occurrence of the entire regular expression on the input.
     *  A mark is inserted at the beginning of the occurrence and the scanner position
     *  is left at the end of the longest string that matches the RE, so that the client
     *  can choose to accept or abort the match. All the ways the occurrence matches
     *  are returned, with capture positions relative to the start of the occurrence.
     *  If no occurrence is found, the scanner goes to the end of the input and
     *  FailedMatch is thrown.
     */
    public List<Match> search(BacktrackScanner s) throws FailedMatch {
        // This implementation avoids backtracking by memoizing hopeless states, so
        // it avoids the typical quadratic worst-case complexity of maximal munch tokenization.
        // Hopelessness depends only on the skeleton state, not on the capture state.
        List<Match> found = null;
        Run run = new Run();
        if (hopelessStates == null) hopelessStates = new HashSet<>();
        Map<Integer, IndexedState> seenStates = new HashMap<>(); // states seen along the way
        s.mark(); // start mark
        int end = s.inputPosition();
        while (s.hasNext()) {
            // if success:  ---------^---------^--------|------
            //                     start.     end.     current
            //
            //.if no success: --------^------------------|-----
            //                      start              current
            try {
                int pos = s.inputPosition();
                int ch = s.nextCodePoint();
                run.step(ch);
                seenStates.put(pos, new IndexedState(pos, run.state));
            } catch (EOF exc) {
                break;
            }
            if (run.state.nullable()) {
                if (found != null) s.accept(); // drop the previous end mark
                found = run.results();
                s.mark(); // end mark, so we can roll back to this position
                end = s.inputPosition();
            } else if (run.state.isVoid() || hopelessStates.contains(new IndexedState(s.inputPosition(), run.state))) {
                if (DEBUG && hopelessStates.contains(new IndexedState(s.inputPosition(), run.state))) {
                    println("Abandoning early because state is hopeless");
                }
                if (found != null) return finishSearch(s, end, seenStates, found);
                s.abort(); // clear old start mark
                s.advance();
                s.mark(); // new start mark
                run = new Run();
            }
        }
        if (found != null) return finishSearch(s, end, seenStates, found);
        throw FailedMatch.exception;
    }

    /** Record the states seen after the last successful end as hopeless, and roll back
     *  to the end mark. */
    private List<Match> finishSearch(BacktrackScanner s, int end, Map<Integer, IndexedState> seenStates,
                                     List<Match> found) {
        int pos = s.inputPosition();
        for (int i = end; i < pos; i++) {
            IndexedState h = seenStates.get(i);
            assert h != null;
            hopelessStates.add(h);
        }
        s.abort(); // roll back to previous end mark
        seenStates.clear();
        return found;
    }
}
