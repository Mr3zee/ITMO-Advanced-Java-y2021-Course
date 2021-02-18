package exceptions;

import java.util.NoSuchElementException;

public class ASNoSuchElementException extends NoSuchElementException {
    @Override
    public String getMessage() {
        return "ArraySet is empty";
    }
}
