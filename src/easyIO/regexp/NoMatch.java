package easyIO.regexp;

public class NoMatch implements Match {
    static NoMatch instance = new NoMatch();
    static Match create() {
        return instance;
    }
}
