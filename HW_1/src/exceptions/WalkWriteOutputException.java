package exceptions;

public class WalkWriteOutputException extends WalkException {
    @Override
    protected String getFilesFormat() {
        return "while writing to output file(s) \"%s\"";
    }
}