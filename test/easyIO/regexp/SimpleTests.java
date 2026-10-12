package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static easyIO.StdIO.println;
import static easyIO.StdIO.readln;
import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.Capture.capture;
import static easyIO.regexp.CharacterClass.anyChar;
import static easyIO.regexp.CharacterClass.range;
import static easyIO.regexp.Concat.concat;
import static easyIO.regexp.Parser.parse;
import static easyIO.regexp.StarRE.star;
import static easyIO.regexp.StringRE.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class SimpleTests {
    public static void main(String[] args) {
        new SimpleTests().run();
    }

    private void run() {
        println("Press Return when ready to start");
        readln();
        test01();
        test02();
        test03();
        test04();
        test05();
        test06();
        test07();
        test08();
        test09();
        test10();
    }

    @Test void test01() {
        RegExp r = string("abc");
        Matcher m = new Matcher(r);
        try {
            m.match("abc");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }

    @Test void test02() {
        RegExp r = alt(string("abc"), string("aba"));
        try {
            new Matcher(r).match("abc");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test03() {
        RegExp r = alt(concat(string("a"), capture(string("b")), string("a")),
                       concat(string("ab"), capture(string("c"))));
        try {
            new Matcher(r).match("abc");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test04() {
        RegExp r = concat(
                string("x"),
                capture(concat(string("b"),
                        alt(string("c"), string("de")),
                        string("f"))),
                string("y"));
        Matcher m = new Matcher(r);
        try {
            m.match("xbdefy");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test05() {
        RegExp r = concat(
                        string("x"),
                        capture(concat(string("b"),
                                        alt(string("c"), string("de"), string("c")),
                                        string("f"))),
                        string("y"));
        Matcher m = new Matcher(r);
        try {
            m.match("xabdefy");
            fail();
        } catch (Matcher.FailedMatch e) {
        }
    }
    @Test void test06() {
        RegExp r = concat(string("x"),
                     capture(star(string("ab"))),
                     string("y"));
        Matcher m = new Matcher(r);
        try {
            m.match("xababy");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test07() {
        // R = (a|aa)*a(a|aa)*
        // Derivative dR =
        // (|a)R|(a|aa)*
        // 2nd: ddR =
        // dR|R|dR
        // = dR|R
        RegExp r = concat(
                star(alt(string("a"), string("aa"))),
                string("a"),
                star(alt(string("a"), string("aa")))
        );
        Matcher m = new Matcher(r);
        try {
             m.match("aaaaaaaaaaaaa");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test08() {
        RegExp r = alt(string("yx"), string("xy"));
        Matcher m = new Matcher(r);
        try {
            m.match("xy");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test09() {
        RegExp r = alt(string("y"), string("y"));
        Matcher m = new Matcher(r);
        try {
            m.match("y");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test10() {
        // R = (a|aa)*a(a|aa)*
        // Derivative dR =
        // (|a)R|(a|aa)*
        // 2nd: ddR =
        // dR|R|dR
        // = dR|R
        RegExp r = concat(
                star(alt(string("a"), string("aa"))),
                string("a"),
                star(alt(string("a"), string("aa")))
        );
        Matcher m = new Matcher(r);
        try {
            m.match("aaaaaaaaaaaaaaaaaaaaaaaaaaaabaaa");
            fail();
        } catch (Matcher.FailedMatch e) {
            // ok
        }
    }
    @Test void test11() {
        RegExp r = concat(string("x"), capture(star(string("ab"))), star(string("a")), string("y"));
        Matcher m = new Matcher(r);
        try {
            m.match("xabay");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test12() {
        RegExp r = concat(capture(star(concat(star(string("a")), string("b")))),
                          capture(star(string("a"))),
                string("x"));
        try {
            new Matcher(r).match("aaaabaaax");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test13() {
        RegExp r = concat(capture(star(concat(star(string("a")), string("b")))),
                capture(star(string("a"))),
                string("x"));
        try {
            new Matcher(r).match("ababx");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test14() {
        RegExp r = star(concat(alt(string("a"), string("aa"))));
        try {
            new Matcher(r).match("aaaaaaaaaaaaaaaaaaaaaaa!");
            fail();
        } catch (Matcher.FailedMatch e) {
        }
    }
    @Test void test15() {
        RegExp r = concat(star(anyChar()), string("x"));
        try {
            new Matcher(r).match("abcdefx");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test16() {
        RegExp r = star(alt(string("a"), string("aa")));
        RegExp s = r.derivative('a');
        RegExp t = s.derivative('a');
        RegExp u = t.derivative('a');
        assertEquals(t, u);
        assertEquals(t.hashCode(), u.hashCode());
        assert t == u;
    }
    @Test void test17() {
        RegExp r = star(alt(string("a"), string("aa")));
        r = concat(r, range('a', 'a'), r);
        RegExp s = r.derivative('a');
        RegExp t = s.derivative('a');
        RegExp u = t.derivative('a');
        assertEquals(t, u);
        assertEquals(t.hashCode(), u.hashCode());
        assert t == u;
    }
    @Test void test18() {
        RegExp r = star(alt(string("a"), string("aa")));
        r = star(concat(r, string("a"), r));
        RegExp s = r.derivative('a');
        RegExp t = s.derivative('a');
        RegExp u = t.derivative('a');
        assertEquals(t, u);
        assertEquals(t.hashCode(), u.hashCode());
        assert t == u;
    }
    @Test void test20() {
        RegExp r = string("abc");
        try {
            BacktrackScanner sc = new BacktrackScanner(new StringReader("xyzw abccdexy"));
            new Matcher(r).search(sc);
            assertEquals("abc", sc.getToken());
            sc.accept();
            assertEquals('c', sc.next());
        } catch (Matcher.FailedMatch|EOF e) {
            fail();
        }
    }
    @Test void test21() {
        RegExp r = concat(star(string("ab")), string("c"));
        try {
            BacktrackScanner sc = new BacktrackScanner(new StringReader("xyzw abababccdexy"));
            new Matcher(r).search(sc);
            assertEquals("abababc", sc.getToken());
            sc.accept();
            assertEquals('c', sc.next());
        } catch (Matcher.FailedMatch|EOF e) {
            fail();
        }
    }
    @Test void test22() {
        RegExp r = alt(concat(star(string("a")), string("b")), string("a"), string("!"));
        try {
            String input = "aaaaaaaa!";
            BacktrackScanner sc = new BacktrackScanner(new StringReader(input));
            Matcher m = new Matcher(r);
            for (int i = 0; i < input.length() - 1; i++) {
                m.search(sc);
//                println("Token: " + sc.getToken());
                assertEquals("a", sc.getToken());
                assertEquals((i < input.length()-2) ? 'a' : '!', sc.peek());
                sc.accept();
            }
            m.search(sc);
            assertEquals("!", sc.getToken());
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test23() throws Parser.SyntaxError {
        RegExp r = parse("abc");
        assertEquals("/abc/", r.toString());
    }
    @Test void test24() throws Parser.SyntaxError {
        RegExp r = parse("ab|c");
        assertEquals("/c|ab/", r.toString());
    }
    @Test void test25() throws Parser.SyntaxError {
        RegExp r = parse("a(ab)*|c");
        assertEquals("/c|a(ab)*/", r.toString());
    }
    @Test void test26() throws Parser.SyntaxError {
        try {
            new Matcher(parse("a(ab)*|c")).match("aabab");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test27() {
        try {
            new Matcher("a(ab)*|c").match("aabab");
            new Matcher("a(ab)*|c").match("a");
            new Matcher("a(ab)*|c").match("c");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test28() throws Parser.SyntaxError {
        RegExp r = parse("ab*");
        assertEquals("/ab*/", r.toString());
    }
    @Test void test29() throws Parser.SyntaxError {
        RegExp r = parse("a|");
        assertEquals("/|a/", r.toString());
    }
    @Test void test30() throws Parser.SyntaxError {
        RegExp r = parse("a{ab*}xx|xc");
        assertEquals("/a{ab*}xx|xc/", r.toString());
        try {
            new Matcher(r).match("aabbbxx");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }

    /** The matches of r against input, as a set of strings like "⟨a⟩1 ⟨b⟩2". */
    private static Set<String> matches(Matcher m, String input) throws Matcher.FailedMatch {
        return m.match(input).stream().map(Match::toString).collect(Collectors.toSet());
    }

    // Example 1 in notes2.txt: (⟦a+a*⟧₁ + ⟦a+a*b⟧₂)b* on ab
    @Test void captures01() throws Matcher.FailedMatch {
        RegExp a = string("a"), b = string("b");
        RegExp r = concat(alt(capture(alt(a, star(a)), 1),
                              capture(alt(a, concat(star(a), b)), 2)),
                          star(b));
        assertEquals(Set.of("⟨a⟩1", "⟨ab⟩2", "⟨a⟩2"), matches(new Matcher(r), "ab"));
    }

    // Example 2 in notes2.txt: ⟦a*⟧₁⟦a*⟧₂ on aaaa
    @Test void captures02() throws Matcher.FailedMatch {
        RegExp a = string("a");
        RegExp r = concat(capture(star(a), 1), capture(star(a), 2));
        assertEquals(Set.of("⟨aaaa⟩1 ⟨⟩2", "⟨aaa⟩1 ⟨a⟩2", "⟨aa⟩1 ⟨aa⟩2", "⟨a⟩1 ⟨aaa⟩2", "⟨⟩1 ⟨aaaa⟩2"),
                     matches(new Matcher(r), "aaaa"));
    }

    // Example 3 in notes2.txt: ⟦(a+aa)*⟧₁ has exponentially many parses but one match
    @Test void captures03() throws Matcher.FailedMatch {
        RegExp r = capture(star(alt(string("a"), string("aa"))));
        String input = "a".repeat(200);
        List<Match> ms = new Matcher(r).match(input);
        assertEquals(1, ms.size());
        assertEquals(input, ms.getFirst().group(1));
    }

    // Example 4 in notes2.txt: (⟦a⟧₁ + ⟦a*⟧₂)b on ab
    @Test void captures04() throws Matcher.FailedMatch {
        RegExp a = string("a");
        RegExp r = concat(alt(capture(a, 1), capture(star(a), 2)), string("b"));
        assertEquals(Set.of("⟨a⟩1", "⟨a⟩2"), matches(new Matcher(r), "ab"));
    }

    // Captures are numbered left to right by the parser
    @Test void captures05() throws Matcher.FailedMatch {
        assertEquals(Set.of("⟨aa⟩1 ⟨⟩2", "⟨a⟩1 ⟨a⟩2", "⟨⟩1 ⟨aa⟩2"),
                     matches(new Matcher("{a*}{a*}"), "aa"));
    }

    // A capture on a branch not taken is unset
    @Test void captures06() throws Matcher.FailedMatch {
        List<Match> ms = new Matcher("a{b}a|ab{c}").match("abc");
        assertEquals(1, ms.size());
        Match m = ms.getFirst();
        assertEquals(null, m.group(1));
        assertEquals("c", m.group(2));
        assertEquals(2, m.start(2));
        assertEquals(3, m.end(2));
        assertEquals("abc", m.group(0));
    }

    // Nested captures
    @Test void captures07() throws Matcher.FailedMatch {
        assertEquals(Set.of("⟨xaby⟩1 ⟨ab⟩2"), matches(new Matcher("{x{ab}y}"), "xaby"));
    }

    // Captures followed by a nullable suffix and a capture-free alternative
    @Test void captures08() throws Matcher.FailedMatch {
        assertEquals(Set.of("⟨a⟩1", ""), matches(new Matcher("{a}b*|ab*"), "abb"));
    }

    @Test void captures09() {
        assertThrows(IllegalArgumentException.class, () -> new Matcher(star(capture(string("a")))));
    }

    @Test void captures10() {
        assertThrows(Matcher.FailedMatch.class, () -> new Matcher("{a}{b}").match("ba"));
    }

    // Search reports the captures of the occurrence found
    @Test void captures11() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("zzxaayzz"));
        List<Match> ms = new Matcher("x{a*}y").search(sc);
        assertEquals("xaay", sc.getToken());
        assertEquals(1, ms.size());
        assertEquals("aa", ms.getFirst().group(1));
    }

    // Search with several nullable points along the way keeps the last one
    @Test void search01() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("xabbbc"));
        new Matcher("ab*").search(sc);
        assertEquals("abbb", sc.getToken());
    }

    // Matching lazily from a scanner gives the same matches as matching a string
    @Test void scannerMatch01() throws Matcher.FailedMatch {
        Matcher m = new Matcher("{a*}{a*}b");
        BacktrackScanner sc = new BacktrackScanner(new StringReader("aab"));
        Set<String> fromScanner = m.match(sc).stream().map(Match::toString).collect(Collectors.toSet());
        assertEquals(matches(m, "aab"), fromScanner);
        assertEquals(3, fromScanner.size());
    }

    /** The tokens found by searching repeatedly with m. */
    private static List<String> tokens(Matcher m, BacktrackScanner sc) {
        List<String> result = new java.util.ArrayList<>();
        try {
            while (true) {
                m.search(sc);
                result.add(sc.getToken());
                sc.accept();
            }
        } catch (Matcher.FailedMatch e) {
            return result;
        }
    }

    // A search that reaches the end of the input without a match retries from the
    // next start position: here the final a.
    @Test void search02() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("ba"));
        new Matcher("(a|b)*c|a").search(sc);
        assertEquals("a", sc.getToken());
        Matcher m = new Matcher("(a|b)*c|a");
        assertEquals(List.of("a", "a", "a"), tokens(m, new BacktrackScanner(new StringReader("aaba"))));
    }

    // What a matcher learns about one scanner's input must not affect another's.
    @Test void search03() {
        Matcher m = new Matcher("ab*c|a");
        assertEquals(List.of("a"), tokens(m, new BacktrackScanner(new StringReader("abbx"))));
        assertEquals(List.of("abbc"), tokens(m, new BacktrackScanner(new StringReader("abbc"))));
    }

    // A failed search leaves the scanner at the end of the input with no mark.
    @Test void search04() {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("xyz"));
        int depth = sc.depth();
        assertThrows(Matcher.FailedMatch.class, () -> new Matcher("ab").search(sc));
        assertEquals(depth, sc.depth());
        assertEquals(false, sc.hasNext());
    }

    @Test void prefix01() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("abbbc"));
        List<Match> ms = new Matcher("ab*").matchPrefix(sc, true);
        assertEquals(1, ms.size());
        assertEquals("abbb", ms.getFirst().group(0));
        assertEquals('c', sc.peek());
    }

    // The longest prefix may be empty
    @Test void prefix02() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("bcd"));
        List<Match> ms = new Matcher("a*").matchPrefix(sc, true);
        assertEquals("", ms.getFirst().group(0));
        assertEquals('b', sc.peek());
    }

    // On failure the position and marks are unchanged
    @Test void prefix03() {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("ac"));
        int depth = sc.depth();
        assertThrows(Matcher.FailedMatch.class, () -> new Matcher("ab").matchPrefix(sc, true));
        assertEquals('a', sc.peek());
        assertEquals(depth, sc.depth());
    }

    // All the ways the prefix matches are reported
    @Test void prefix04() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("aax"));
        Set<String> ms = new Matcher("{a*}{a*}").matchPrefix(sc, true).stream()
                .map(Match::toString).collect(Collectors.toSet());
        assertEquals(Set.of("⟨aa⟩1 ⟨⟩2", "⟨a⟩1 ⟨a⟩2", "⟨⟩1 ⟨aa⟩2"), ms);
        assertEquals('x', sc.peek());
    }

    // Without captures, a single match with no capture groups is reported
    @Test void prefix05() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("aax"));
        List<Match> ms = new Matcher("{a*}{a*}").matchPrefix(sc, false);
        assertEquals(1, ms.size());
        assertEquals(0, ms.getFirst().groupCount());
        assertEquals("aa", ms.getFirst().group(0));
        assertEquals('x', sc.peek());
    }

    // Finding the longest prefix reads past it, then rolls back
    @Test void prefix06() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("aaaax"));
        int depth = sc.depth();
        assertEquals("a", new Matcher("a*b|a").matchPrefix(sc, true).getFirst().group(0));
        assertEquals(1, sc.inputPosition());
        assertEquals(depth, sc.depth());
    }

    // An unmatched ')' is a syntax error, rather than ending the pattern early
    @Test void parse01() {
        for (String p : List.of("a)", "a))", "ab)c", ")")) {
            assertThrows(Parser.SyntaxError.class, () -> parse(p), p);
        }
    }

    // Unmatched or misplaced metacharacters are syntax errors
    @Test void parse02() {
        for (String p : List.of("{a", "a}", "{a)", "(a}", "*a", "a|*", "]", "a\\")) {
            assertThrows(Parser.SyntaxError.class, () -> parse(p), p);
        }
    }

    // Braces capture; nested parentheses only group
    @Test void parse03() throws Matcher.FailedMatch {
        assertEquals(Set.of("⟨ab⟩1"), matches(new Matcher("{((a|b)*)}c"), "abc"));
        assertEquals(Set.of(""), matches(new Matcher("((a|b)*)c"), "abc"));
    }

    // A backslash makes any character literal, and printing escapes metacharacters
    @Test void parse04() throws Parser.SyntaxError, Matcher.FailedMatch {
        RegExp r = parse("\\{a\\*\\}\\\\");
        assertEquals("/\\{a\\*\\}\\\\/", r.toString());
        new Matcher(r).match("{a*}\\");
        assertEquals(Set.of("⟨(x)⟩1"), matches(new Matcher("{\\(x\\)}"), "(x)"));
    }

    private static boolean matchesAll(String pattern, String input) {
        try {
            new Matcher(pattern).match(input);
            return true;
        } catch (Matcher.FailedMatch e) {
            return false;
        }
    }

    @Test void class01() {
        assertTrue(matchesAll("[abc]*", "abccba"));
        assertFalse(matchesAll("[abc]*", "abd"));
        assertTrue(matchesAll("[a-z_][a-z0-9_]*", "x_42"));
        assertFalse(matchesAll("[a-z_][a-z0-9_]*", "4x"));
        assertTrue(matchesAll("[^0-9]*", "abc"));
        assertFalse(matchesAll("[^0-9]*", "ab3"));
        assertTrue(matchesAll("[^]", "λ"));       // any character, including non-ASCII
        assertFalse(matchesAll("[]", ""));        // matches nothing
        assertFalse(matchesAll("[]", "a"));
        assertTrue(matchesAll("[α-ω]*", "λμ"));
    }

    // '-' is literal at the start or end; other metacharacters can be escaped
    @Test void class02() {
        assertTrue(matchesAll("[-a]*", "a-a"));
        assertTrue(matchesAll("[a-]*", "-a"));
        assertTrue(matchesAll("[\\]\\-\\\\^]*", "]-\\^"));
        assertFalse(matchesAll("[\\]\\-]", "a"));
        assertTrue(matchesAll("[*|(){}]*", "*|(){}")); // other metacharacters are literal in a class
    }

    @Test void class03() {
        for (String p : List.of("[a", "[a-", "[z-a]", "[\\", "[^")) {
            assertThrows(Parser.SyntaxError.class, () -> parse(p), p);
        }
    }

    // Classes are canonical: equal sets are equal, and printing round-trips
    @Test void class04() throws Parser.SyntaxError {
        assertSame(parse("[a-cb-dx]"), parse("[xa-d]"));
        assertEquals("/[a-dx]/", parse("[xa-d]").toString());
        assertEquals("/[^a]/", parse("[^a]").toString());
        assertEquals("/[^]/", parse("[^]").toString());
        assertEquals("/a/", parse("[a]").toString()); // a single character is just a string
        for (String p : List.of("[a-dx]", "[^0-9]", "[\\]\\-\\\\]", "[ab]")) {
            RegExp r = parse(p);
            assertSame(r, parse(r.toString().substring(1, r.toString().length() - 1)), p);
        }
    }

    @Test void class05() throws Matcher.FailedMatch {
        assertEquals(Set.of("⟨key⟩1 ⟨v4l⟩2"), matches(new Matcher("{[a-z]*}=[ ]*{[a-z0-9]*}"), "key=  v4l"));
    }

    // '.' is any character except a line break
    @Test void dot01() {
        assertTrue(matchesAll("a.c", "abc"));
        assertTrue(matchesAll("a.c", "aλc"));
        assertFalse(matchesAll("a.c", "a\nc"));
        assertFalse(matchesAll("a.c", "a\rc"));
        assertTrue(matchesAll("a\\.c", "a.c"));
        assertFalse(matchesAll("a\\.c", "abc"));
        assertTrue(matchesAll("[.]", "."));           // literal inside a class
    }

    // With longest-prefix matching, .* stops at the end of the line
    @Test void dot02() throws Matcher.FailedMatch {
        BacktrackScanner sc = new BacktrackScanner(new StringReader("# comment\nx = 1"));
        assertEquals("# comment", new Matcher("#.*").matchPrefix(sc, false).getFirst().group(0));
        assertEquals('\n', sc.peek());
    }

    @Test void escape01() {
        assertTrue(matchesAll("a\\nb", "a\nb"));
        assertTrue(matchesAll("\\t\\r", "\t\r"));
        assertTrue(matchesAll("[\\n\\t ]*", " \t\n "));
        assertTrue(matchesAll("[^\\n]*", "abc"));
        assertFalse(matchesAll("[^\\n]*", "a\nb"));
    }

    // A backslash before a letter or digit other than n, r, t is reserved
    @Test void escape02() {
        for (String p : List.of("\\d", "\\w", "\\1", "[\\d]", "a\\x")) {
            assertThrows(Parser.SyntaxError.class, () -> parse(p), p);
        }
    }

    // Printing uses '.' and escape sequences, and round-trips
    @Test void escape03() throws Parser.SyntaxError {
        assertEquals("/a.c/", parse("a.c").toString());
        assertEquals("/a\\.c/", parse("a\\.c").toString());
        assertEquals("/\\n\\t/", parse("\\n\\t").toString());
        assertEquals("/[\\t\\n ]/", parse("[ \\n\\t]").toString());
        for (String p : List.of("a.c", "a\\.c", "\\n\\t", "[ \\n\\t]", "[^\\n]x")) {
            RegExp r = parse(p);
            String printed = r.toString();
            assertSame(r, parse(printed.substring(1, printed.length() - 1)), p);
        }
    }

    // Combining separately parsed patterns can give two captures the same number,
    // which is rejected rather than silently losing one of them
    @Test void duplicateCaptures01() throws Parser.SyntaxError {
        RegExp r = concat(parse("{a}"), parse("{b}"));
        assertThrows(IllegalArgumentException.class, () -> new Matcher(r));
    }

    // Numbering captures apart when parsing allows the patterns to be combined
    @Test void duplicateCaptures02() throws Parser.SyntaxError, Matcher.FailedMatch {
        RegExp r = concat(parse("{a}", 1), parse("{b}{c}", 2));
        assertEquals(Set.of("⟨a⟩1 ⟨b⟩2 ⟨c⟩3"), matches(new Matcher(r), "abc"));
        assertThrows(IllegalArgumentException.class, () -> parse("{a}", 0));
    }

    // Identical alternatives with the same capture are merged by alt, so they are
    // not duplicates; the same unnumbered capture used twice gets two numbers
    @Test void duplicateCaptures03() throws Parser.SyntaxError, Matcher.FailedMatch {
        RegExp r = alt(parse("{a}"), parse("{a}"));
        assertEquals(Set.of("⟨a⟩1"), matches(new Matcher(r), "a"));
        RegExp c = capture(string("a"));
        assertEquals(Set.of("⟨a⟩1 ⟨a⟩2"), matches(new Matcher(concat(c, c)), "aa"));
    }

    // An empty alternative matches the empty string; '|' is never literal
    @Test void emptyAlternative() {
        assertTrue(matchesAll("|a", ""));
        assertTrue(matchesAll("|a", "a"));
        assertFalse(matchesAll("|a", "|a"));
        assertTrue(matchesAll("x(|a)y", "xy"));
        assertTrue(matchesAll("x(|a)y", "xay"));
        assertTrue(matchesAll("a||b", ""));
        assertTrue(matchesAll("a||b", "b"));
        assertTrue(matchesAll("a|", ""));
        assertTrue(matchesAll("{|a}", ""));
        assertTrue(matchesAll("\\|", "|"));
    }

    // Captures built with the API are numbered like parsed ones: an enclosing
    // capture before the captures inside it
    @Test void nestedCaptureNumbering() throws Parser.SyntaxError, Matcher.FailedMatch {
        RegExp built = capture(concat(capture(string("a")), string("b")));
        assertEquals(Set.of("⟨ab⟩1 ⟨a⟩2"), matches(new Matcher(built), "ab"));
        assertEquals(Set.of("⟨ab⟩1 ⟨a⟩2"), matches(new Matcher(parse("{{a}b}")), "ab"));
        RegExp deeper = concat(capture(concat(capture(string("x")), capture(string("y")))), capture(string("z")));
        assertEquals(Set.of("⟨xy⟩1 ⟨x⟩2 ⟨y⟩3 ⟨z⟩4"), matches(new Matcher(deeper), "xyz"));
        assertEquals(Set.of("⟨xy⟩1 ⟨x⟩2 ⟨y⟩3 ⟨z⟩4"), matches(new Matcher(parse("{{x}{y}}{z}")), "xyz"));
    }
}
