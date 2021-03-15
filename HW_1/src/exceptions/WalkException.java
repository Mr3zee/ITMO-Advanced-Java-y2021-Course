package exceptions;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class WalkException extends Exception {
    private String[] files;
    private String name;
    private String message;

    protected WalkException() {
    }

    WalkException init(final String name, final String message, final String... files) {
        this.files = files;
        this.name = name;
        this.message = message;
        return this;
    }

    public static <T extends WalkException> T create(Class<T> clazz, String name, String message, String... files) {
        try {
            return (T) clazz.getDeclaredConstructor().newInstance().init(name, message, files);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to create WalkException", e);
        }
    }
    // TODO: 15.02.2021 maybe final

    public static <T extends WalkException> T create(Class<T> clazz, Exception exc, String... files) {
        return create(clazz, exc.getClass().getSimpleName(), exc.getMessage(), files);
    }

    @Override
    public String getMessage() {
        return String.format("Exception was caught => %s\n---- Details: %s => %s\n", getFormattedMessage(), this.name, this.message);
    }

    protected String getFormattedMessage() {
        return files.length != 0
                ? String.format(getFilesFormat(),
                Arrays.stream(this.files)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")))
                : getDefaultMessage();
    }

    protected String getDefaultMessage() {
        return "";
    }

    protected String getFilesFormat() {
        return "while working with file(s): \"%s\"";
    }

    public void print() {
        print(System.err);
    }

    public void print(final PrintStream out) {
        out.print(getMessage());
    }
}
