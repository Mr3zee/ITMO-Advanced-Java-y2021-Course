package exceptions;

public class ASUnsupportedOperationException extends UnsupportedOperationException {
    public ASUnsupportedOperationException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return String.format("ArraySet is immutable. Operation \"%s\" is not supported", super.getMessage());
    }
}
