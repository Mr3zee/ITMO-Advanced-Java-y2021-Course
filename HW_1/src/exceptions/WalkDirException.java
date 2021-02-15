package exceptions;

public class WalkDirException extends WalkException {
    @Override
    protected String getFilesFormat() {
        return "while walking dir(s) \"%s\"";
    }
}
