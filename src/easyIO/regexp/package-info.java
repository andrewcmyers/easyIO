/** Regular expressions with captures, matched using Brzozowski derivatives. The
 *  main classes to know about are:
 *  <ul>
 *  <li>{@code Parser}: parses the textual syntax of regular expressions, which is
 *      documented there. In brief: {@code R|S}, {@code RS}, {@code R*},
 *      {@code (R)} for grouping, {@code {R}} for a capture, character classes
 *      {@code [...]}, {@code .} for any character except a line break, and
 *      backslash escapes.
 *  <li>{@code Matcher}: matches a regular expression against a whole string or
 *      scanner input ({@code match}), the longest prefix at the current position
 *      ({@code matchPrefix}), or the first occurrence ({@code search}).
 *  <li>{@code Match}: one way the expression matched, giving the text captured by
 *      each capture group.
 *  <li>{@code RegExp} and its subclasses: regular expressions, which can also be
 *      built directly with factory methods such as {@code Concat.concat},
 *      {@code AlternationRE.alt}, {@code StarRE.star}, and {@code Capture.capture}.
 *  </ul>
 *  The convenience methods {@code next}, {@code hasNext}, and {@code nextMatches}
 *  of {@code easyIO.Scanner} use this package to scan input matching a pattern.
 *
 *  <p>Matching never backtracks: it takes time linear in the length of the input,
 *  and constant stack space. Rather than choosing one match, a matcher reports
 *  all the ways the expression matches, each with its own captures. A capture
 *  may not occur inside a Kleene star.
 *
 *  <p>Regular expressions are interned, and their derivatives are computed lazily
 *  and cached, forming a state machine that is shared by all matchers. These
 *  caches are safe to use from multiple threads, and a {@code Matcher} may be
 *  shared by several threads. They are never discarded, so a program that
 *  constructs many distinct regular expressions uses memory that grows
 *  accordingly.
 *
 * @author Andrew Myers
 */
package easyIO.regexp;
