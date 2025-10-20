package easyIO.regexp;

import org.junit.jupiter.api.Test;

import static easyIO.StdIO.println;
import static easyIO.StdIO.readln;
import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.Capture.capture;
import static easyIO.regexp.CharacterClass.anyChar;
import static easyIO.regexp.CharacterClass.range;
import static easyIO.regexp.Concat.concat;
import static easyIO.regexp.StarRE.star;
import static easyIO.regexp.StringRE.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SimpleTests {
    public static void main(String[] args) {
        new SimpleTests().run();
    }

    private void run() {
        println("Press Enter when ready to start");
        readln();
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test9();
        test10();
        for (int i = 1; i < 100000; i++) {
            test10();
        }
        readln();
    }

    @Test void test1() {
        RegExp r = string("abc");
        Matcher m = new Matcher(r);
        try {
            m.match("abc");
        } catch (Matcher.FailedMatch e) {
            println("No match");
        }
    }

    @Test void test2() {
        RegExp r = alt(string("abc"), string("aba"));
        try {
            new Matcher(r).match("abc");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test3() {
        RegExp r = alt(concat(string("a"), capture(string("b")), string("a")),
                       concat(string("ab"), capture(string("c"))));
        try {
            new Matcher(r).match("abc");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test4() {
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
            println("No match");
            fail();
        }
    }
    @Test void test5() {
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
            println("No match");
        }
    }
    @Test void test6() {
        RegExp r = concat(string("x"),
                     capture(star(string("ab"))),
                     string("y"));
        Matcher m = new Matcher(r);
        try {
            m.match("xababy");
        } catch (Matcher.FailedMatch e) {
            println("No match");
            fail();
        }
    }
    @Test void test7() {
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
    @Test void test8() {
        RegExp r = alt(string("yx"), string("xy"));
        Matcher m = new Matcher(r);
        try {
            m.match("xy");
        } catch (Matcher.FailedMatch e) {
            fail();
        }
    }
    @Test void test9() {
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
}
