package exceptions;

public class WalkFileVisitException extends WalkException {
    @Override
    protected String getFilesFormat() {
        return "while visiting file(s) in directory (dir is the first one) \"%s\"";
    }
}