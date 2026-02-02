package easyIO.regexp;

import java.util.List;

/** A match associated with an alternation expression.
 *
 */
public class AltMatch implements Match {
    List<Match> cases;
}