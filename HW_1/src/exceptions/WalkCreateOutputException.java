package exceptions;

public class WalkCreateOutputException extends WalkException {
    @Override
    protected String getFilesFormat() {
        return "while creating output file(s) (directory(s)): \"%s\"";
    }
}
