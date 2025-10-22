package easyIO.regexp;

import easyIO.BacktrackScanner;
import easyIO.EOF;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

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
        RegExp r = parse("a((ab*))xx|xc");
        assertEquals("/a((ab*))xx|xc/", r.toString());
        try {
            new Matcher(r).match("aabbbxx");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
}
