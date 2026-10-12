package easyIO.regexp;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.Capture.capture;
import static easyIO.regexp.Concat.concat;
import static easyIO.regexp.StarRE.star;
import static easyIO.regexp.StringRE.string;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Interned regular expressions and their caches are shared by all threads. Check
 *  that matching many fresh patterns concurrently, so that the threads race to fill
 *  the same cold caches, gives the same results as matching them sequentially.
 */
public class ConcurrencyTests {
    private static final int THREADS = 8;
    private static final String ALPHABET = "abλ"; // λ is not ASCII, so the maps are used too

    /** A random pattern over ALPHABET with captures, numbered from next[0]. */
    private static RegExp randomPattern(Random rnd, int depth, boolean captureOK, int[] next) {
        int choice = rnd.nextInt(depth <= 0 ? 2 : 6);
        RegExp c = string(String.valueOf(ALPHABET.charAt(rnd.nextInt(ALPHABET.length()))));
        return switch (choice) {
            case 0, 1 -> c;
            case 2 -> concat(randomPattern(rnd, depth - 1, captureOK, next),
                             randomPattern(rnd, depth - 1, captureOK, next));
            case 3 -> alt(randomPattern(rnd, depth - 1, captureOK, next),
                          randomPattern(rnd, depth - 1, captureOK, next));
            case 4 -> star(randomPattern(rnd, depth - 1, false, next));
            default -> captureOK ? capture(concat(c, randomPattern(rnd, depth - 1, true, next)), next[0]++) : c;
        };
    }

    private static String randomInput(Random rnd) {
        StringBuilder b = new StringBuilder();
        int n = rnd.nextInt(12);
        for (int i = 0; i < n; i++) b.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        return b.toString();
    }

    /** The matches as a sorted list of strings, or "fail". */
    private static String run(Matcher m, String input) {
        try {
            return m.match(input).stream().map(Match::toString).sorted().collect(Collectors.joining(";"));
        } catch (Matcher.FailedMatch e) {
            return "fail";
        }
    }

    @Test void concurrentMatchingAgreesWithSequential() throws Exception {
        Random rnd = new Random(42);
        int cases = 400;
        RegExp[] patterns = new RegExp[cases];
        String[] inputs = new String[cases];
        for (int i = 0; i < cases; i++) {
            // distinct prefixes keep the caches of each pattern cold
            patterns[i] = concat(string("x" + i), randomPattern(rnd, 4, true, new int[]{1}));
            inputs[i] = "x" + i + randomInput(rnd);
        }
        Matcher[] matchers = Arrays.stream(patterns).map(Matcher::new).toArray(Matcher[]::new);

        // All threads start together and go through the cases in different orders.
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        List<Future<String[]>> futures = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            final int seed = t;
            futures.add(pool.submit(() -> {
                List<Integer> order = new ArrayList<>();
                for (int i = 0; i < cases; i++) order.add(i);
                Collections.shuffle(order, new Random(seed));
                String[] results = new String[cases];
                start.await();
                for (int i : order) results[i] = run(matchers[i], inputs[i]);
                return results;
            }));
        }
        List<String[]> concurrent = new ArrayList<>();
        for (Future<String[]> f : futures) concurrent.add(f.get(60, TimeUnit.SECONDS));
        pool.shutdown();

        for (int i = 0; i < cases; i++) {
            String expected = run(matchers[i], inputs[i]);
            for (String[] results : concurrent) {
                assertEquals(expected, results[i], "pattern " + patterns[i] + " on " + inputs[i]);
            }
        }
    }
}
