package exceptions;

public class WalkFormatException extends WalkException {
    @Override
    protected String getDefaultMessage() {
        return "Invalid file format";
    }
}
