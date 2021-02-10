package exceptions;

public class WalkWriteOutputException extends WalkException {
    @Override
    protected String getTypeFormat() {
        return "while writing to output file \"%s\"";
    }
}