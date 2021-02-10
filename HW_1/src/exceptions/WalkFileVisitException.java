package exceptions;

public class WalkFileVisitException extends WalkException {
    @Override
    protected String getTypeFormat() {
        return "while visiting file \"%s\"";
    }
}