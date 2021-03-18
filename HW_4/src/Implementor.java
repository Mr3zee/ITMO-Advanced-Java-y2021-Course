import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
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
        if (clazz == null) {
            throw new ImplerException("Token cannot be null");
        }
        if (clazz.isPrimitive()
                || clazz.isArray()
                || clazz.isAnnotation()
                || clazz.isEnum()
                || clazz.isAssignableFrom(Enum.class)) {
            throw new ImplerException(
                    "Not supported class to implement: " + clazz.getName()
            );
        }
        StringBuilder builder = new StringBuilder();
        createClass(clazz, builder);

        Path path = createOutputFile(root, getImplClassFullName(clazz));
        try {
            Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ImplerException("Unable to write to file");
        }
    }

    private void createClass(final Class<?> clazz, final StringBuilder builder) throws ImplerException {
        try {
            String className = clazz.getSimpleName() + "Impl";
            builder.append(clazz.getPackage()).append(";").append(nl2)
                    .append("public class ").append(className)
                    .append(clazz.isInterface()
                            ? " implements "
                            : " extends ")
                    .append(clazz.getName()).append(" {").append(nl2);
            createConstructors(clazz, builder, className);
            createMethods(clazz, builder);
            builder.append("}");
        } catch (UncheckedImplerException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    private void createConstructors(final Class<?> clazz, final StringBuilder builder, final String className) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            Class<?>[] args = constructor.getParameterTypes();
            addMethod(
                    builder,
                    className,
                    args,
                    constructor.isVarArgs(),
                    "",
                    getSuperStatement(args.length),
                    false,
                    0
            );
        }
    }

    private String getSuperStatement(int n) {
        return "super(" +
                IntStream.range(0, n)
                        .mapToObj(i -> "arg" + i)
                        .collect(Collectors.joining(", "))
                + ");";
    }

    private void createMethods(final Class<?> clazz, final StringBuilder builder) {
        createMethods(clazz.getMethods(), clazz.isInterface(), builder);
        createMethods(
                Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> Modifier.isProtected(m.getModifiers()))
                        .toArray(Method[]::new),
                clazz.isInterface(),
                builder
        );
    }

    private void createMethods(final Method[] methods, boolean isInterface, final StringBuilder builder) {
        for (Method method : methods) {
            addMethod(
                    builder,
                    method.getName(),
                    method.getParameterTypes(),
                    method.isVarArgs(),
                    getType(method.getReturnType()),
                    getReturnStatement(method),
                    !isInterface,
                    method.getModifiers()
            );
        }
    }

    private static class UncheckedImplerException extends RuntimeException {
        public UncheckedImplerException(String message) {
            super(message);
        }
    }

    private String getType(final Class<?> clazz) {
        if (clazz.isArray()) {
            return getType(clazz.getComponentType()) + "[]";
        }
        if (clazz.isMemberClass()) {
            if (isPrivateMember(clazz)) {
                throw new UncheckedImplerException(
                        "Unable to implement due to private inner class as required type: " + clazz.getName()
                );
            }
            return clazz.getName().replace('$', '.');
        }
        return clazz.getName();
    }

    private boolean isPrivateMember(Class<?> clazz) {
        return Modifier.isPrivate(clazz.getModifiers()) ||
                (clazz.isMemberClass() && isPrivateMember(clazz.getDeclaringClass()));
    }

    private String getReturnStatement(final Method method) {
        Class<?> type =  method.getReturnType();
        return type.isAssignableFrom(void.class) ? ""
                : "return " + (type.isPrimitive()
                ? type.isAssignableFrom(boolean.class)
                ? "false"
                : "0"
                : null) + ";";
    }

    private void addMethod(
            final StringBuilder builder,
            final String name,
            final Class<?>[] args,
            boolean isVarArgs,
            final String returnType,
            final String body,
            boolean isOverride,
            int flags
    ) {
        if (Modifier.isFinal(flags)
                || Modifier.isStatic(flags)
                || Modifier.isNative(flags)
        ) {
            return;
        }
        if (isOverride) {
            builder.append("\t@Override").append(nl);
        }
        builder.append("\t").append(Modifier.isProtected(flags) ? "protected" : "public").append(" ")
                .append(returnType).append(" ").append(name).append("(")
                .append(getStringParameters(args, isVarArgs))
                .append(") { ").append(body).append(" }")
                .append(nl2);
    }

    private String getStringParameters(final Class<?>[] args, boolean isVarArgs) {
        return IntStream.range(0, args.length)
                .mapToObj(i -> (isVarArgs && i == args.length - 1
                        ? getType(args[i].getComponentType()) + "..."
                        : getType(args[i])
                ) + " arg" + i)
                .collect(Collectors.joining(", "));
    }

    private String getImplClassFullName(final Class<?> clazz) {
        return clazz.getName().replace('.', '\\') + "Impl.java";
    }

    private Path createOutputFile(final Path root, final String classFullName) throws ImplerException {
        if (root == null) {
            throw new ImplerException("Root cannot be null");
        }
        return createFile(Path.of(root.toString(), classFullName));
    }

    private Path createFile(final Path file) throws ImplerException {
        try {
            if (!Files.exists(file)) {
                final Path parent = file.getParent();
                Files.createDirectories(parent);
                Files.createFile(file);
            } else {
                Files.writeString(file, "");
            }
            return file;
        } catch (IOException e) {
            throw new ImplerException("Unable to create output file");
        }
    }
}
