package exceptions;

public class WalkCreateOutputException extends WalkException {
    @Override
    protected String getTypeFormat() {
        return "while creating output file/directories to file \"%s\"";
    }
}
