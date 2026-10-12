package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;
import easyIO.regexp.Ops.Env;
import easyIO.regexp.RegExp.DerivTerm;

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

    /** For each scanner searched with this matcher, the states known not to lead to a
     *  match. Input positions are only meaningful for the scanner they were learned
     *  from, so the states are kept separately for each scanner. The keys are weak so
     *  that a scanner can be garbage-collected, and the map is synchronized so that a
     *  Matcher can be used by several threads at once. A scanner itself must be used
     *  by only one thread at a time. */
    private final Map<BacktrackScanner, Set<IndexedState>> hopelessStates =
            Collections.synchronizedMap(new WeakHashMap<>());

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
        static final FailedMatch exception = new FailedMatch();
    }

    /** One run of the matcher over some input: the current skeleton state and the
     *  capture state aligned with its positions. The capture state is null when the
     *  regular expression has no captures, in which case the state is just the
     *  Brzozowski derivative. */
    private final class Run {
        RegExp state = expr;
        /** For each position of the state, its environments. These arrays are never
         *  modified once built, so they can be shared between steps. */
        Env[][] captures;
        /** The number of chars consumed so far. */
        int pos = 0;
        /** The text consumed so far, when it must be recorded because the input
         *  is not available as a CharSequence; otherwise null. */
        final StringBuilder text;

        Run(boolean recordText) {
            text = recordText ? new StringBuilder() : null;
            if (expr.hasCaptures()) {
                Env[] initial = { Env.empty(numCaptures) };
                captures = new Env[summands(expr).length][];
                Arrays.fill(captures, initial);
            }
        }

        void step(int codepoint) {
            if (captures == null) {
                state = state.derivative(codepoint);
            } else {
                Transition t = transition(state, codepoint);
                Env[][] next = new Env[t.sources.length][];
                for (int j = 0; j < t.sources.length; j++) {
                    int[] sources = t.sources[j];
                    Ops[] ops = t.ops[j];
                    if (sources.length == 1) {
                        next[j] = apply(ops[0], captures[sources[0]], pos);
                    } else {
                        // Several edges land here: the union is R+R=R.
                        Set<Env> envs = new LinkedHashSet<>();
                        for (int e = 0; e < sources.length; e++) {
                            for (Env env : captures[sources[e]]) envs.add(ops[e].apply(env, pos));
                        }
                        next[j] = envs.toArray(NO_ENVS);
                    }
                }
                state = t.target;
                captures = next;
            }
            if (text != null) text.appendCodePoint(codepoint);
            if (DEBUG) println(pos + ". State is now " + state + ", captures " + Arrays.deepToString(captures));
            pos += Character.charCount(codepoint);
        }

        /** The matches if the input ends here, in no particular order. Empty if the
         *  input seen so far does not match. */
        List<Match> results() {
            return results(text.toString());
        }

        /** The matches if the input ends here, where s is the input consumed. */
        List<Match> results(String s) {
            if (!state.nullable()) return List.of();
            if (captures == null) return List.of(new Match(s, Env.empty(0)));
            Ops[][] accept = acceptOps(state);
            Set<Env> envs = new LinkedHashSet<>();
            for (int i = 0; i < accept.length; i++) {
                for (Ops ops : accept[i]) {
                    for (Env env : captures[i]) envs.add(ops.apply(env, s.length()));
                }
            }
            return envs.stream().map(env -> new Match(s, env)).toList();
        }
    }

    private static final Env[] NO_ENVS = {};

    /** The environments obtained by applying ops at pos to each of envs. Applying ops
     *  to distinct environments from the same position yields distinct environments,
     *  because each capture fires at most once on any path: every environment of a
     *  term containing ⟦R⟧k has k unmapped, and every environment of a term containing
     *  ⟨R⟧k has k open. So no duplicates need to be removed, and when there are no
     *  ops the array can be shared. */
    private static Env[] apply(Ops ops, Env[] envs, int pos) {
        if (ops == Ops.NONE) return envs;
        Env[] result = new Env[envs.length];
        for (int i = 0; i < envs.length; i++) result[i] = ops.apply(envs[i], pos);
        assert new HashSet<>(Arrays.asList(result)).size() == result.length : "duplicate environments";
        return result;
    }

    /** The (cached) transition of matcher state `state` on `codepoint`. */
    static Transition transition(RegExp state, int codepoint) {
        if (codepoint < RegExp.ASCII) {
            if (state.asciiTransitions == null) state.asciiTransitions = new Transition[RegExp.ASCII];
            Transition t = state.asciiTransitions[codepoint];
            if (t == null) {
                t = computeTransition(state, codepoint);
                state.asciiTransitions[codepoint] = t;
            }
            return t;
        }
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
        Ops[][] result = state.acceptOps;
        if (result == null) {
            result = Arrays.stream(summands(state))
                    .map(r -> r.nullOps().toArray(new Ops[0]))
                    .toArray(Ops[][]::new);
            state.acceptOps = result;
        }
        return result;
    }

    /** All the ways the regular expression matches the entire input s, in no
     *  particular order. Throws FailedMatch if there are none. */
    public List<Match> match(CharSequence s) throws FailedMatch {
        Run run = new Run(false);
        int n = s.length();
        for (int i = 0; i < n; ) {
            int ch = Character.codePointAt(s, i);
            run.step(ch);
            if (run.state.isVoid()) {
                throw FailedMatch.exception;
            }
            i += Character.charCount(ch);
        }
        List<Match> result = run.results(s.toString());
        if (result.isEmpty()) throw FailedMatch.exception;
        return result;
    }

    /** All the ways the regular expression matches the entire input, in no
     *  particular order. Throws FailedMatch if there are none. The input is
     *  read lazily, one character at a time. */
    public List<Match> match(BacktrackScanner s) throws FailedMatch {
        Run run = new Run(true);
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
     *  Occurrences of the empty string are not found.
     *  If no occurrence is found, the scanner goes to the end of the input, no mark is
     *  left, and FailedMatch is thrown.
     */
    public List<Match> search(BacktrackScanner s) throws FailedMatch {
        // This implementation avoids backtracking by memoizing hopeless states: a state
        // reached at a given input position from which no match can end. Without the
        // memo, maximal munch tokenization has quadratic worst-case complexity, since
        // each attempt may scan to the end of the input. Hopelessness depends only on
        // the skeleton state, not on the capture state, and positions are only
        // meaningful for this scanner.
        Set<IndexedState> hopeless = hopelessStates.computeIfAbsent(s, k -> new HashSet<>());
        List<Match> found = null;
        Run run = new Run(true);
        // The state after each position since `end` (or since the start, if there is no
        // match yet), keyed by the input position just after reading a character.
        Map<Integer, IndexedState> seenStates = new HashMap<>();
        s.mark(); // start mark
        int end = s.inputPosition();
        while (true) {
            // if success:  ---------^---------^--------|------
            //                     start.     end.     current
            //
            //.if no success: --------^------------------|-----
            //                      start              current
            boolean stuck; // whether no longer match can be found from this start
            if (!s.hasNext()) {
                stuck = true;
            } else {
                int ch;
                try {
                    ch = s.nextCodePoint();
                } catch (EOF exc) {
                    throw new Error("cannot happen: hasNext() was true");
                }
                run.step(ch);
                IndexedState here = new IndexedState(s.inputPosition(), run.state);
                if (run.state.nullable()) {
                    if (found != null) s.accept(); // drop the previous end mark
                    found = run.results();
                    s.mark(); // end mark, so we can roll back to this position
                    end = s.inputPosition();
                    seenStates.clear();
                    continue;
                }
                seenStates.put(here.inputPosition(), here);
                stuck = run.state.isVoid() || hopeless.contains(here);
                if (DEBUG && !run.state.isVoid() && stuck) println("Abandoning early because state is hopeless");
            }
            if (!stuck) continue;
            // None of the states seen since `end` (or since the start) leads to a match.
            hopeless.addAll(seenStates.values());
            seenStates.clear();
            if (found != null) {
                s.abort(); // roll back to the end mark
                return found;
            }
            s.abort(); // roll back to the start mark and erase it
            if (!s.hasNext()) throw FailedMatch.exception;
            s.advance();
            if (!s.hasNext()) throw FailedMatch.exception;
            s.mark(); // new start mark
            run = new Run(true);
            end = s.inputPosition();
        }
    }
}
