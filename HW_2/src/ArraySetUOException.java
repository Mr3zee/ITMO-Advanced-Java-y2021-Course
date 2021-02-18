public class ArraySetUOException extends UnsupportedOperationException {
    public ArraySetUOException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return String.format("ArraySet is immutable. Operation \"%s\" is not supported", super.getMessage());
    }
}
