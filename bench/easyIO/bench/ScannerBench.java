package easyIO.bench;

import easyIO.regexp.Matcher;

import java.io.StringReader;
import java.util.Random;
import java.util.regex.Pattern;

/** Compare easyIO.Scanner with java.util.Scanner on common scanning tasks. Each task
 *  computes a checksum so that the two scanners can be checked for agreement; a
 *  mismatch is reported. Times are the best of several runs, after a warm-up run.
 *
 *  Run with: ./gradlew bench
 */
public class ScannerBench {
    interface Work { long run() throws Exception; }

    static void row(String name, int reps, Work easy, Work java) throws Exception {
        long e = easy.run(), j = java.run(); // warm up, and check agreement
        if (e != j) System.out.println("  MISMATCH in " + name + ": " + e + " vs " + j);
        double te = time(reps, easy), tj = time(reps, java);
        System.out.printf("%-38s easyIO %8.1f ms   java.util %8.1f ms   ratio %5.2f%n",
                name, te, tj, te / tj);
    }

    static double time(int reps, Work w) throws Exception {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < reps; i++) {
            long t0 = System.nanoTime();
            w.run();
            best = Math.min(best, System.nanoTime() - t0);
        }
        return best / 1e6;
    }

    static easyIO.Scanner easy(String s) { return new easyIO.Scanner(new StringReader(s), "input"); }
    static java.util.Scanner java(String s) { return new java.util.Scanner(new StringReader(s)); }

    public static void main(String[] args) throws Exception {
        Random rnd = new Random(1);
        int reps = 5;

        // 1M integers separated by spaces and newlines
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1_000_000; i++) {
            sb.append(rnd.nextInt(2_000_000) - 1_000_000).append(i % 10 == 9 ? '\n' : ' ');
        }
        String ints = sb.toString();
        row("1M ints: nextInt", reps,
            () -> { var sc = easy(ints); long sum = 0;
                    while (true) { sc.whitespace(); if (!sc.hasNext()) break; sum += sc.nextInt(); }
                    return sum; },
            () -> { var sc = java(ints); long sum = 0;
                    while (sc.hasNext()) sum += sc.nextInt();
                    return sum; });

        // 300k doubles
        sb.setLength(0);
        for (int i = 0; i < 300_000; i++) {
            sb.append(String.format("%.4f", rnd.nextDouble() * 1000 - 500)).append(i % 8 == 7 ? '\n' : ' ');
        }
        String doubles = sb.toString();
        row("300k doubles: nextDouble", reps,
            () -> { var sc = easy(doubles); double sum = 0;
                    while (true) { sc.whitespace(); if (!sc.hasNext()) break; sum += sc.nextDouble(); }
                    return Math.round(sum * 1000); },
            () -> { var sc = java(doubles); double sum = 0;
                    while (sc.hasNext()) sum += sc.nextDouble();
                    return Math.round(sum * 1000); });

        // 200k lines of ~40 characters
        sb.setLength(0);
        for (int i = 0; i < 200_000; i++) {
            int len = 20 + rnd.nextInt(40);
            for (int j = 0; j < len; j++) sb.append((char) ('a' + rnd.nextInt(26)));
            sb.append('\n');
        }
        String lines = sb.toString();
        row("200k lines: nextLine", reps,
            () -> { var sc = easy(lines); long total = 0;
                    while (sc.hasNext()) total += sc.nextLine().length() - 1; // easyIO includes the newline
                    return total; },
            () -> { var sc = java(lines); long total = 0;
                    while (sc.hasNextLine()) total += sc.nextLine().length();
                    return total; });

        // 500k identifiers separated by whitespace
        sb.setLength(0);
        for (int i = 0; i < 500_000; i++) {
            sb.append((char) ('a' + rnd.nextInt(26)));
            int len = rnd.nextInt(10);
            for (int j = 0; j < len; j++) sb.append("abcxyz_0123456789".charAt(rnd.nextInt(17)));
            sb.append(i % 12 == 11 ? '\n' : ' ');
        }
        String idents = sb.toString();
        row("500k identifiers: nextIdentifier/next", reps,
            () -> { var sc = easy(idents); long total = 0;
                    while (true) { sc.whitespace(); if (!sc.hasNext()) break; total += sc.nextIdentifier().length(); }
                    return total; },
            () -> { var sc = java(idents); long total = 0;
                    while (sc.hasNext()) total += sc.next().length();
                    return total; });

        // The same identifiers, matched against a pattern
        Matcher ident = new Matcher("[a-z_][a-z0-9_]*");
        Pattern jident = Pattern.compile("[a-z_][a-z0-9_]*");
        row("500k identifiers: next(pattern)", reps,
            () -> { var sc = easy(idents); long total = 0;
                    while (true) { sc.whitespace(); if (!sc.hasNext()) break; total += sc.next(ident).length(); }
                    return total; },
            () -> { var sc = java(idents); long total = 0;
                    while (sc.hasNext()) total += sc.next(jident).length();
                    return total; });

        // Mixed tokens: count those matching a pattern, skip the rest (hasNext(pattern))
        sb.setLength(0);
        for (int i = 0; i < 300_000; i++) {
            if (rnd.nextBoolean()) sb.append(rnd.nextInt(100000));
            else for (int j = 0; j < 1 + rnd.nextInt(8); j++) sb.append((char) ('a' + rnd.nextInt(26)));
            sb.append(' ');
        }
        String mixed = sb.toString();
        Matcher num = new Matcher("[0-9][0-9]*");
        Matcher word = new Matcher("[a-z][a-z]*");
        Pattern jnum = Pattern.compile("[0-9]+");
        row("300k mixed tokens: hasNext(pattern)", reps,
            () -> { var sc = easy(mixed); long count = 0;
                    while (true) {
                        sc.whitespace(); if (!sc.hasNext()) break;
                        if (sc.hasNext(num)) { sc.next(num); count++; } else sc.next(word);
                    }
                    return count; },
            () -> { var sc = java(mixed); long count = 0;
                    while (sc.hasNext()) {
                        if (sc.hasNext(jnum)) { sc.next(jnum); count++; } else sc.next();
                    }
                    return count; });
    }
}
