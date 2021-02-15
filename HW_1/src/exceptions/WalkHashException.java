package exceptions;

public class WalkHashException extends WalkException {
    @Override
    protected String getFilesFormat() {
        return "while calculating hash for \"%s\" file(s)";
    }
}
