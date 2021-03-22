import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Implementor implements Impler {
    private static final String nl = String.format("%n");
    private static final String nl2 = nl + nl;
    private final Map<TypeParameter, TypeParameter> typesMap = new HashMap<>();
    private BufferedWriter writer;

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

    private static Path getPath(final String path) throws ImplerException {
        try {
            return Path.of(path);
        } catch (InvalidPathException e) {
            throw new ImplerException("Wrong path: " + path);
        }
    }

    private static Class<?> getClass(final String clazz) throws ImplerException {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Wrong class name: " + clazz);
        }
    }

    public void implement(final Path path, final Class<?>... clazzes) throws ImplerException {
        for (Class<?> clazz : clazzes) {
            implement(clazz, path);
        }
    }

    @Override
    public void implement(final Class<?> clazz, final Path root) throws ImplerException {
        if (clazz == null) {
            throw new ImplerException("Token cannot be null");
        }
        if (clazz.isPrimitive()
                || clazz.isArray()
                || clazz.isEnum()
                || clazz.isAssignableFrom(Enum.class)
        ) {
            throw new ImplerException(
                    "Not supported class to implement: " + clazz.getName()
            );
        }
        Path path = createOutputFile(root, getImplClassFullName(clazz));
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            this.writer = writer;
            createClass(clazz);
        } catch (UncheckedImplerException | IOException e) {
            try {
                Files.delete(path);
            } catch (IOException e2) {
                throw new ImplerException("Unable to delete file after error occurred:\n" + e2.getMessage());
            }
            throw new ImplerException(e.getMessage() + " || class: " + clazz.getName());
        }
    }

    private void createClass(final Class<?> clazz) {
        try {
            if (Modifier.isFinal(clazz.getModifiers())) {
                throw new UncheckedImplerException("Unable to implement final class");
            }
            typesMap.clear();
            createTypesMap(clazz);

            String className = clazz.getSimpleName() + "Impl";
            TypeVariable<? extends Class<?>>[] typeParameters = clazz.getTypeParameters();

            StringBuilder builder = new StringBuilder();

            builder.append(clazz.getPackage()).append(";").append(nl2)
                    .append("public class ").append(className)
                    .append(createFullTypeParameters(typeParameters, null))
                    .append(clazz.isInterface()
                            ? " implements "
                            : " extends ")
                    .append(clazz.isMemberClass() ? getMemberClassName(clazz) : clazz.getName())
                    .append(createTypeParameters(typeParameters, TypeVariable::getName))
                    .append("{").append(nl2);

            writer.write(builder.toString());

            if (!clazz.isInterface()) {
                createConstructors(clazz, className);
            }
            createMethods(clazz);

            writer.write("}");
        } catch (IOException e) {
            throw new UncheckedImplerException(e.getMessage());
        }
    }

    private void createTypesMap(final Class<?> clazz) {
        Class<?> superClass = createTypesMap(clazz, clazz.getGenericSuperclass());
        if (superClass != null) {
            createTypesMap(superClass);
        }
        for (Type superInterfaceType : clazz.getGenericInterfaces()) {
            Class<?> superInterface = createTypesMap(clazz, superInterfaceType);
            if (superInterface != null) {
                createTypesMap(superInterface);
            }
        }
    }

    private Class<?> createTypesMap(final Class<?> clazz, final Type superType) {
        if (superType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) superType;
            Class<?> superClazz = (Class<?>) parameterizedType.getRawType();

            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Type[] superParameters = superClazz.getTypeParameters();

            IntStream.range(0, superParameters.length).forEach(i -> {
                TypeParameter key = new TypeParameter(superParameters[i], superClazz);
                TypeParameter value = new TypeParameter(actualTypeArguments[i], clazz);
                typesMap.put(key, value);
            });
            return superClazz;
        }
        return null;
    }

    private String createFullTypeParameters(final TypeVariable<?>[] typeVariables, final Class<?> declaringClass) {
        return createTypeParameters(typeVariables, this::createFullTypeParameter, declaringClass);
    }

    private String createTypeParameters(
            final TypeVariable<?>[] typeVariables,
            final Function<TypeVariable<?>, String> converter
    ) {
        return createTypeParameters(typeVariables, (t, ignored) -> converter.apply(t), null);
    }

    private String createTypeParameters(
            final TypeVariable<?>[] typeVariables,
            final BiFunction<TypeVariable<?>, Class<?>, String> converter,
            final Class<?> declaringClass
    ) {
        return typeVariables.length == 0 ? "" : "<"
                + join(Arrays.stream(typeVariables).map(t -> converter.apply(t, declaringClass))) + ">";
    }

    private String createFullTypeParameter(final TypeVariable<?> variable, final Class<?> declaringClass) {
        return getFullTypeDeclarationUpper(variable.getTypeName(), variable.getBounds(), declaringClass);
    }

    private void createConstructors(
            final Class<?> clazz,
            final String className
    ) {
        boolean noAvailableConstructor = true;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            int flags = constructor.getModifiers();
            Type[] args = constructor.getGenericParameterTypes();
            if (forbiddenExecutable(flags)) {
                continue;
            }
            Class<?> declaringClass = constructor.getDeclaringClass();
            addMethod(
                    createFullTypeParameters(constructor.getTypeParameters(), declaringClass),
                    className,
                    args,
                    constructor.isVarArgs(),
                    "",
                    getSuperStatement(args.length),
                    constructor.getGenericExceptionTypes(),
                    flags,
                    false,
                    declaringClass
            );
            noAvailableConstructor = false;
        }
        if (noAvailableConstructor) {
            throw new UncheckedImplerException("No available constructor to create");
        }
    }

    private static String getSuperStatement(int n) {
        return "super(" + join(IntStream.range(0, n).mapToObj(i -> "arg" + i)) + ");";
    }

    private void createMethods(final Class<?> clazz) {
        createMethods(getAllAccessibleMethods(clazz));
    }

    private static Stream<Method> getAllAccessibleMethods(final Class<?> clazz) {
        Package actualPackage = clazz.getPackage();
        Set<MethodWrapper> wrappers = new HashSet<>();
        Set<MethodWrapper> forbidden = new HashSet<>();
        for (Method method : clazz.getMethods()) {
            addMethod(wrappers, forbidden, method, method.getModifiers());
        }
        for (Class<?> level = clazz; level != null; level = level.getSuperclass()) {
            Package levelPackage = level.getPackage();
            for (Method method : level.getDeclaredMethods()) {
                int flags = method.getModifiers();
                if (Modifier.isPublic(flags)
                        || Modifier.isPrivate(flags)
                        || method.getAnnotation(Deprecated.class) != null && !Modifier.isAbstract(flags)
                ) {
                    continue;
                }
                if (Modifier.isProtected(flags)
                        || (actualPackage == null && levelPackage == null)
                        || levelPackage.equals(actualPackage)) {
                    addMethod(wrappers, forbidden, method, flags);
                }
            }
        }
        return wrappers.stream().map(MethodWrapper::getMethod);
    }

    private static void addMethod(
            final Set<MethodWrapper> methods,
            final Set<MethodWrapper> forbidden,
            final Method newMethod,
            int flags
    ) {
        MethodWrapper wrapper = new MethodWrapper(newMethod);
        if (Modifier.isFinal(flags)) {
            forbidden.add(wrapper);
            return;
        }
        if (!newMethod.isSynthetic() && !forbidden.contains(wrapper) && !forbiddenExecutable(flags)) {
            methods.add(wrapper);
        }
    }

    private void createMethods(final Stream<Method> methods) {
        methods.forEach(method -> {
            Class<?> declaringClass = method.getDeclaringClass();
            addMethod(
                    createFullTypeParameters(method.getTypeParameters(), declaringClass),
                    method.getName(),
                    method.getGenericParameterTypes(),
                    method.isVarArgs(),
                    getType(method.getGenericReturnType(), declaringClass),
                    getReturnStatement(method),
                    method.getGenericExceptionTypes(),
                    method.getModifiers(),
                    true,
                    declaringClass
            );
        });
    }

    private static boolean forbiddenExecutable(int flags) {
        return Modifier.isFinal(flags)
                || Modifier.isStatic(flags)
                || Modifier.isNative(flags)
                || Modifier.isPrivate(flags);
    }

    private String getFullTypeDeclaration(
            final String name,
            final String word,
            final Type[] bounds,
            final Class<?> declaringClass
    ) {
        return name + (bounds.length == 1 && bounds[0] == Object.class ? ""
                : " " + word + " " + join(Arrays.stream(bounds).map(t -> getType(t, declaringClass)), " & "));
    }

    private String getFullTypeDeclarationUpper(final String name, final Type[] bounds, final Class<?> declaringClass) {
        return getFullTypeDeclaration(name, "extends", bounds, declaringClass);
    }

    private String getFullTypeDeclarationLower(final Type[] bounds, final Class<?> declaringClass) {
        return getFullTypeDeclaration("?", "super", bounds, declaringClass);
    }

    private String getType(final Type type, final Class<?> declaringClass) {
        if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return getType(arrayType.getGenericComponentType(), declaringClass) + "[]";
        }
        if (type instanceof TypeVariable) {
            return substituteType(type, declaringClass);
        }
        if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            Type[] lowerBounds = wildcard.getLowerBounds();
            Type[] upperBounds = wildcard.getUpperBounds();
            return lowerBounds.length == 0
                    ? getFullTypeDeclarationUpper("?", upperBounds, declaringClass)
                    : getFullTypeDeclarationLower(lowerBounds, declaringClass);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return getType(parameterizedType.getRawType(), declaringClass)
                    + "<" + join(Arrays.stream(parameterizedType.getActualTypeArguments())
                    .map(t -> getType(t, declaringClass))) + ">";
        }
        Class<?> clazz = (Class<?>) type;
        if (clazz.isMemberClass()) {
            return getMemberClassName(clazz);
        }
        return substituteType(type, declaringClass);
    }

    private String substituteType(final Type type, final Class<?> declaringClass) {
        TypeParameter parameter = new TypeParameter(type, declaringClass);
        while (true) {
            TypeParameter value = typesMap.get(parameter);
            if (value == null) {
                return parameter.getType().getTypeName();
            }
            parameter = value;
        }
    }

    private static String getMemberClassName(final Class<?> clazz) {
        if (!clazz.isMemberClass()) {
            return clazz.getName();
        }
        if (Modifier.isPrivate(clazz.getModifiers())) {
            throw new UncheckedImplerException(
                    "Unable to implement due to private inner class as required type: " + clazz.getName()
            );
        }
        return getMemberClassName(clazz.getDeclaringClass()) + "." + clazz.getSimpleName();
    }

    private String getReturnStatement(final Method method) {
        Class<?> type = method.getReturnType();
        return type.isAssignableFrom(void.class) ? "" : "return " +
                (type.isPrimitive() ? (type.isAssignableFrom(boolean.class) ? "false" : "0") : null) + ";";
    }

    private static String getModifier(int flags) {
        return Modifier.toString(flags & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }

    private static String addWhitespaceIfNotEmpty(String string) {
        return string + (string.equals("") ? "" : " ");
    }

    private void addMethod(
            final String genericTypeParameters,
            final String name,
            final Type[] args,
            boolean isVarArgs,
            final String returnType,
            final String body,
            final Type[] exceptions,
            int flags,
            boolean isOverride,
            final Class<?> declaringClass
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(isOverride ? "\t@Override" + nl : "")
                .append("\t").append(addWhitespaceIfNotEmpty(getModifier(flags)))
                .append(addWhitespaceIfNotEmpty(genericTypeParameters))
                .append(addWhitespaceIfNotEmpty(returnType)).append(name).append("(")
                .append(getStringParameters(args, isVarArgs, declaringClass)).append(") ")
                .append(addWhitespaceIfNotEmpty(getExceptionSignature(exceptions, declaringClass)))
                .append("{ ").append(addWhitespaceIfNotEmpty(body)).append("}")
                .append(nl2);
        try {
            writer.write(builder.toString());
        } catch (IOException e) {
            throw new UncheckedImplerException(e.getMessage());
        }
    }

    private String getExceptionSignature(final Type[] exceptions, final Class<?> declaringClass) {
        return exceptions.length == 0 ? "" : "throws "
                + join(Arrays.stream(exceptions)
                .map(t -> getType(t, declaringClass)));
    }

    private String getStringParameters(final Type[] args, boolean isVarArgs, final Class<?> declaringClass) {
        return join(
                IntStream.range(0, args.length)
                        .mapToObj(i -> (isVarArgs && i == args.length - 1
                                ? getVarArgsType(args[i], declaringClass)
                                : getType(args[i], declaringClass)
                        ) + " arg" + i)
        );
    }

    private String getVarArgsType(final Type arrayType, final Class<?> declaringClass) {
        return (arrayType instanceof GenericArrayType
                ? getType(((GenericArrayType) arrayType).getGenericComponentType(), declaringClass)
                : getType(((Class<?>) arrayType).getComponentType(), declaringClass)
        ) + "...";
    }

    private static String getImplClassFullName(final Class<?> clazz) {
        return (clazz.getPackageName() + "." + clazz.getSimpleName()).replace('.', '\\') + "Impl.java";
    }

    private static Path createOutputFile(final Path root, final String classFullName) throws ImplerException {
        if (root == null) {
            throw new ImplerException("Root cannot be null");
        }
        return createFile(Path.of(root.toString(), classFullName));
    }

    private static Path createFile(final Path file) throws ImplerException {
        try {
            if (!Files.exists(file)) {
                final Path parent = file.getParent();
                if (parent == null) {
                    throw new IOException();
                }
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

    private static String join(final Stream<String> stream, final String delimiter) {
        return stream.collect(Collectors.joining(delimiter));
    }

    private static String join(final Stream<String> stream) {
        return join(stream, ", ");
    }

    private static class MethodWrapper {
        private final Method method;
        private final String name;
        private final Type[] types;

        public MethodWrapper(final Method method) {
            this.method = method;
            this.name = method.getName();
            this.types = method.getGenericParameterTypes();
        }

        public Method getMethod() {
            return method;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodWrapper)) return false;
            MethodWrapper that = (MethodWrapper) o;
            return name.equals(that.name) && Arrays.equals(types, that.types);
        }

        @Override
        public int hashCode() {
            return 53 * name.hashCode() + Arrays.hashCode(types);
        }
    }

    private static class TypeParameter {
        private final Type type;
        private final Class<?> clazz;

        private TypeParameter(final Type type, final Class<?> clazz) {
            this.type = type;
            this.clazz = clazz;
        }

        public Type getType() {
            return type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof TypeParameter)) return false;
            TypeParameter that = (TypeParameter) o;
            return Objects.equals(type, that.type) && Objects.equals(clazz, that.clazz);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, clazz);
        }
    }

    private static class UncheckedImplerException extends RuntimeException {
        public UncheckedImplerException(final String message) {
            super(message);
        }
    }

}
