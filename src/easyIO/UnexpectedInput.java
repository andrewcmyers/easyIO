package easyIO;

import easyIO.BacktrackScanner.Location;

/** Represents encountering input that does not match the expected syntax. */
@SuppressWarnings("serial")
public class UnexpectedInput extends Exception {
    BacktrackScanner.Location location;
    public UnexpectedInput() {}
    public UnexpectedInput(String message, Location loc) {
        super(loc + ":" + message);
        location = loc;
    }
}
