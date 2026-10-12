package easyIO;

import easyIO.regexp.Match;
import easyIO.regexp.Matcher;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Tests of the regular-expression methods of Scanner: next, hasNext and nextMatches. */
public class ScannerPatternTests {
    private static Scanner scanner(String input) {
        return new Scanner(new StringReader(input), "test");
    }

    @Test void nextLongestPrefix() throws UnexpectedInput {
        Scanner sc = scanner("abbbc");
        assertEquals("abbb", sc.next("ab*"));
        assertEquals('c', sc.peek());
    }

    @Test void nextTokens() throws UnexpectedInput {
        // Paralleling java.util.Scanner.next(pattern), with explicit whitespace skipping.
        Scanner sc = scanner("abc  aab\n ba c");
        Matcher word = new Matcher("(a|b)*c|(a|b)(a|b)*");
        List<String> tokens = new ArrayList<>();
        while (true) {
            sc.whitespace();
            if (!sc.hasNext()) break;
            tokens.add(sc.next(word));
        }
        assertEquals(List.of("abc", "aab", "ba", "c"), tokens);
    }

    @Test void nextFailureLeavesPosition() {
        Scanner sc = scanner("ac");
        int depth = sc.depth();
        assertThrows(UnexpectedInput.class, () -> sc.next("ab"));
        assertEquals('a', sc.peek());
        assertEquals(depth, sc.depth());
    }

    @Test void nextEmptyMatch() throws UnexpectedInput {
        Scanner sc = scanner("bcd");
        assertEquals("", sc.next("a*"));
        assertEquals('b', sc.peek());
    }

    @Test void hasNextDoesNotMove() {
        Scanner sc = scanner("abbc");
        int depth = sc.depth();
        assertTrue(sc.hasNext("ab*c"));
        assertFalse(sc.hasNext("b"));
        assertEquals('a', sc.peek());
        assertEquals(depth, sc.depth());
    }

    @Test void nextMatchesCaptures() throws UnexpectedInput {
        Scanner sc = scanner("aax");
        Set<String> ms = sc.nextMatches(new Matcher("{a*}{a*}")).stream()
                .map(Match::toString).collect(Collectors.toSet());
        assertEquals(Set.of("⟨aa⟩1 ⟨⟩2", "⟨a⟩1 ⟨a⟩2", "⟨⟩1 ⟨aa⟩2"), ms);
        assertEquals('x', sc.peek());
    }

    @Test void nextMatchesGroups() throws UnexpectedInput {
        Scanner sc = scanner("key=value;");
        List<Match> ms = sc.nextMatches(new Matcher("{k(e|y)*}={v(a|l|u|e)*}"));
        assertEquals(1, ms.size());
        Match m = ms.getFirst();
        assertEquals("key=value", m.group(0));
        assertEquals("key", m.group(1));
        assertEquals("value", m.group(2));
        assertEquals(';', sc.peek());
    }

    @Test void nextMatchesFailure() {
        Scanner sc = scanner("xyz");
        assertThrows(UnexpectedInput.class, () -> sc.nextMatches(new Matcher("{a}")));
        assertEquals('x', sc.peek());
    }

    @Test void invalidPattern() {
        Scanner sc = scanner("abc");
        assertThrows(IllegalArgumentException.class, () -> sc.next("(a"));
    }

    @Test void nextWithClasses() throws UnexpectedInput {
        Scanner sc = scanner("count = 42;");
        assertEquals("count", sc.next("[a-zA-Z_][a-zA-Z0-9_]*"));
        sc.whitespace();
        assertEquals("=", sc.next("[=]"));
        sc.whitespace();
        assertFalse(sc.hasNext("[a-z]"));
        assertEquals("42", sc.next("[0-9][0-9]*"));
        assertEquals(';', sc.peek());
    }
}
