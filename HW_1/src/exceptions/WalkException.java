package exceptions;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public abstract class WalkException extends Exception {
    private Path file;
    private String name;
    private String message;

    protected WalkException() {}

    WalkException init(Path file, String name, String message) {
        this.file = file;
        this.name = name;
        this.message = message;
        return this;
    }

    public static <T extends WalkException> T create(Class<T> clazz, Path file, String name, String message) {
        try {
            return (T) clazz.getDeclaredConstructor().newInstance().init(file, name, message);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to create WalkException", e);
        }
    }

    public static <T extends WalkException> T create(Class<T> clazz, Path file, Exception exc) {
        return create(clazz, file, exc.getClass().getSimpleName(), exc.getMessage());
    }

    @Override
    public String getMessage() {
        return String.format("I/O Exception was caught: %s\n---> Details: %s -> %s\n", getType(), this.name, this.message);
    }

    protected String getType() {
        return String.format(getTypeFormat(), this.file);
    }

    protected abstract String getTypeFormat();

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        out.print(getMessage());
    }
}
