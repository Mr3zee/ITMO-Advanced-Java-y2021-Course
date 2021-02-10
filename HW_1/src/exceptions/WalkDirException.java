package exceptions;

public class WalkDirException extends WalkException {
    @Override
    protected String getTypeFormat() {
        return "while walking dir \"%s\"";
    }
}
