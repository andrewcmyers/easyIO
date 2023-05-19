package easyIO;

/** Represents encountering input that does not match the expected syntax. */
@SuppressWarnings("serial")
public class UnexpectedInput extends Exception {
    public UnexpectedInput() {}
    public UnexpectedInput(String message) {
        super(message);
    }
}
