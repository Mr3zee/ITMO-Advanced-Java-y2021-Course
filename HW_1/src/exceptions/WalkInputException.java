package exceptions;

public class WalkInputException extends WalkException {
    @Override
    protected String getFilesFormat() {
        return "while opening input file(s) \"%s\"";
    }
}