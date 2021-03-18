import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Implementor implements Impler {
    private final String nl;
    private final String nl2;

    public Implementor() {
        this.nl = System.lineSeparator();
        this.nl2 = this.nl + this.nl;
    }

    public void run(final String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong Number of arguments");
            return;
        }
        try {
            Class<?> clazz = getClass(args[0]);
            Path path = getPath(args[1]);
            implement(clazz, path);
        } catch (ImplerException e) {
            System.out.println(e.getMessage());
        }
    }

    private Path getPath(final String path) throws ImplerException {
        try {
            return Path.of(path);
        } catch (InvalidPathException e) {
            throw new ImplerException("Wrong path");
        }
    }

    private Class<?> getClass(final String clazz) throws ImplerException {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Wrong class name");
        }
    }

    @Override
    public void implement(Class<?> clazz, Path root) throws ImplerException {
//        createOutputFile(root, getImplClassFullName(clazz));
        StringBuilder builder = new StringBuilder();
        createClass(clazz, builder);
        System.out.println(builder.toString());
    }

    private void createClass(final Class<?> clazz, final StringBuilder builder) {
        String className = clazz.getSimpleName() + "Impl";
        builder.append("public class ").append(className).append(" {").append(nl2);
        createConstructors(clazz, builder, className);
        createMethods(clazz, builder);
        builder.append("}");
    }

    private void createConstructors(final Class<?> clazz, final StringBuilder builder, final String className) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            addMethod(builder, className, constructor.getParameterTypes(), "", "");
            builder.append(nl2);
        }
    }

    private void createMethods(final Class<?> clazz, final StringBuilder builder) {
        for (Method method : clazz.getMethods()) {
            addMethod(
                    builder,
                    method.getName(),
                    method.getParameterTypes(),
                    method.getReturnType().getName(),
                    getReturnValue(method)
            );
            builder.append(nl2);
        }
    }

    private String getReturnValue(final Method method) {
        Class<?> type =  method.getReturnType();
        return type.isPrimitive()
                ? type.isAssignableFrom(Boolean.class)
                    ? "false"
                    : type.isAssignableFrom(void.class)
                        ? ""
                        : "0"
                : null;
    }

    private void addMethod(
            final StringBuilder builder,
            final String name,
            final Class<?>[] args,
            final String returnType,
            final Object returnValue
    ) {
        builder.append("\tpublic ").append(returnType).append(" ").append(name).append("(")
                .append(getStringParameters(args))
                .append(") { return ").append(returnValue).append("; }");
    }

    private String getStringParameters(final Class<?>[] args) {
        return args.length == 0 ? ""
                : IntStream.of(0, args.length - 1)
                .mapToObj(i -> args[i].getName() + " arg" + i)
                .collect(Collectors.joining(", "));
    }

    private String getImplClassFullName(final Class<?> clazz) {
        return clazz.getName().replace('.', '\\') + "Impl.java";
    }

    private void createOutputFile(final Path root, final String classFullName) throws ImplerException {
        createFile(Path.of(root.toString(), classFullName));
    }

    private void createFile(final Path file) throws ImplerException {
        try {
            final Path parent = file.getParent();
            Files.createDirectories(parent);
            Files.createFile(file);
        } catch (IOException e) {
            throw new ImplerException("Unable to create output file");
        }
    }
}
