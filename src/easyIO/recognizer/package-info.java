/** Recognizers: composable matchers for input on a {@code BacktrackScanner}, built
 *  from combinators and matched by backtracking. The main classes to know about are:
 *  <ul>
 *  <li>{@code Recognizer}: the interface of a recognizer, which invokes a
 *      continuation for each way it can recognize the input at the current position.
 *  <li>{@code Regex}: combinators for building recognizers (concatenation,
 *      alternation, repetition, constants, character sets) and methods for using
 *      them to scan input.
 *  <li>{@code PredictiveRecognizer}: a recognizer that chooses another recognizer
 *      based on the next input character.
 *  </ul>
 *  Because recognizers backtrack, they can take time exponential in the length of
 *  the input on some patterns, but they can contain arbitrary code. For regular
 *  expressions with captures that are matched in linear time without backtracking,
 *  see the package {@code easyIO.regexp}.
 *
 * @author Andrew Myers
 */
package easyIO.recognizer;
