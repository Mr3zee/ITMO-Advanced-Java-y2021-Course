package exceptions;

public class WalkHashException extends WalkException {
    @Override
    protected String getTypeFormat() {
        return "while calculating hash for \"%s\" file";
    }
}
