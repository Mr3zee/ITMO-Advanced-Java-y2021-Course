package exceptions;

public class ASSubsetIndexException extends IllegalArgumentException {
    @Override
    public String getMessage() {
        return "\"From\" element should be less than \"To\" element";
    }
}
