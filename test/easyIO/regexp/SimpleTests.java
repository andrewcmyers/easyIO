package easyIO.regexp;

import org.junit.jupiter.api.Test;

import static easyIO.StdIO.println;
import static easyIO.regexp.AlternationRE.alt;
import static easyIO.regexp.Capture.capture;
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
        test2();
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
        Matcher m = new Matcher(r);
        try {
            Match match = m.match("abc");
            println("Match is " + match);
        } catch (Matcher.FailedMatch e) {
            assertEquals("match", e);
        }
    }
    @Test void test3() {
        RegExp r = alt(concat(string("a"), capture(string("b")), string("a")),
                       concat(string("ab"), capture(string("c"))));
        Matcher m = new Matcher(r);
        try {
            Match match = m.match("abc");
            println("Match is " + match);
        } catch (Matcher.FailedMatch e) {
            println("No match");
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
            Match match = m.match("xbdefy");
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
            Match match = m.match("xabdefy");
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
            Match match = m.match("xababy");
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
            Match match = m.match("aaaaaaaaaaaaa");
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
}
