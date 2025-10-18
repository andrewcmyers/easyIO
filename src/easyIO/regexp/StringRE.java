package easyIO.regexp;

public class EmptyStringRE extends RegExp {
    private static final RegExp theRE = new EmptyStringRE();

    public static RegExp create() {
        return theRE;
    }

    protected RegExp computeDerivative(int character) {
        return VoidRE.create();
    }
}
