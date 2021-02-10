package exceptions;

public class WalkInputException extends WalkException {
    @Override
    protected String getTypeFormat() {
        return "while opening input file \"%s\"";
    }
}