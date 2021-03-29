import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementor class is able to create new .java and .jar files
 */
public class Implementor implements Impler, JarImpler {
    /**
     * System dependent new line character
     */
    private static final String nl = String.format("%n");
    /**
     * Double system dependent new line character
     */
    private static final String nl2 = nl + nl;
    /**
     * TypesMap contains all type substitutions in inheritance tree for given class
     */
    private final Map<TypeParameter, TypeParameter> typesMap = new HashMap<>();
    /**
     * Output Stream to write class implementation
     */
    private ImplementorOutputStream outputStream;

    /**
     * Creates buffered output stream to write implementation to
     * @param clazz class to implement
     * @param root root path for class
     * @return new {@link ImplementorOutputStream} for given class
     * @throws IOException if unable to create output file
     */
    private static ImplementorOutputStream getBufferedOutputStream(final Class<?> clazz, final Path root) throws IOException {
        Path path = createOutputFile(root, getImplClassFullName(clazz));
        return new ImplementorOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(path.toFile())
                ),
                path
        );
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        implement(token, root, Implementor::getBufferedOutputStream);
    }

    /**
     * Implements given class if is able to
     *
     * @param clazz class to implement
     * @param path path to pass to getter
     * @param getter function to get stream to write implementation to
     * @throws ImplerException if unable to implement clazz
     */
    public void implement(final Class<?> clazz, final Path path, final OutputStreamGetter getter) throws ImplerException {
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
        try (ImplementorOutputStream stream = getter.getStream(clazz, path)) {
            this.outputStream = stream;
            createClass(clazz);
        } catch (UncheckedImplerException | IOException e) {
            try {
                Files.delete(outputStream.getPath());
            } catch (IOException e2) {
                throw new ImplerException("Unable to delete file after error occurred:\n" + e2.getMessage());
            }
            throw new ImplerException(e.getMessage() + " || class: " + clazz.getName());
        }
    }

    /**
     * Generates java code for new implementation
     *
     * @param clazz class to implement
     * @throws UncheckedImplerException if unable to implement
     */
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

            outputStream.write(builder.toString());

            if (!clazz.isInterface()) {
                createConstructors(clazz, className);
            }
            createMethods(clazz);

            outputStream.write("}");
        } catch (IOException e) {
            throw new UncheckedImplerException(e.getMessage());
        }
    }

    /**
     * Walks inheritance tree for given class and makes types substitution map
     *
     * @param clazz class to implement
     */
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

    /**
     * Creates entries for type substitution for class and its superclass if is able to
     *
     * @param clazz class to create entries for
     * @param superType class's parent type
     * @return parent if entries were created
     */
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

    /**
     * Creates type parameters with bounds relatively to declaring class
     * @param typeVariables array of types to work with
     * @param declaringClass class, in which parameters were declared
     * @return type parameters in form &lt;T1, T2...&gt;
     */
    private String createFullTypeParameters(final TypeVariable<?>[] typeVariables, final Class<?> declaringClass) {
        return createTypeParameters(typeVariables, this::createFullTypeParameter, declaringClass);
    }

    /**
     * Creates type parameters with bounds ignoring declaring class
     * @param typeVariables array of types to work with
     * @param converter converts type into java code (string)
     * @return type parameters in form &lt;T1, T2...&gt;
     */
    private static String createTypeParameters(
            final TypeVariable<?>[] typeVariables,
            final Function<TypeVariable<?>, String> converter
    ) {
        return createTypeParameters(typeVariables, (t, ignored) -> converter.apply(t), null);
    }

    /**
     * Creates type parameters with bounds relatively to declaring class
     * @param typeVariables array of types to work with
     * @param converter converts type into java code (string) relatively to declaring class
     * @param declaringClass class, in which parameters were declared
     * @return type parameters in form &lt;T1, T2...&gt;
     */
    private static String createTypeParameters(
            final TypeVariable<?>[] typeVariables,
            final BiFunction<TypeVariable<?>, Class<?>, String> converter,
            final Class<?> declaringClass
    ) {
        return typeVariables.length == 0 ? "" : "<"
                + join(Arrays.stream(typeVariables).map(t -> converter.apply(t, declaringClass))) + ">";
    }

    /**
     * Creates one full type parameter relatively to declaring class
     * @param variable variable to work with
     * @param declaringClass class, in which variable was declared
     * @return full type parameter: T extends E1 & E2 ...
     */
    private String createFullTypeParameter(final TypeVariable<?> variable, final Class<?> declaringClass) {
        return getFullTypeDeclarationUpper(variable.getTypeName(), variable.getBounds(), declaringClass);
    }

    /**
     * Create all constructors for given class
     * @param clazz class to implement
     * @param className name for constructors
     */
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

    /**
     * Creates super statement for constructor
     * @param n number of args
     * @return super(arg0, arg1,... argn)
     */
    private static String getSuperStatement(int n) {
        return "super(" + join(IntStream.range(0, n).mapToObj(i -> "arg" + i)) + ");";
    }

    /**
     * Create all methods for given class
     * @param clazz class to implement
     */
    private void createMethods(final Class<?> clazz) {
        createMethods(getAllAccessibleMethods(clazz));
    }

    /**
     * Returns all methods, that can be overriden for given class
     * @param clazz to search methods in
     * @return {@link Stream} of {@link Method} that can be overriden
     */
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

    /**
     * Adds method wrapped in {@link MethodWrapper} to set if method is valid
     * @param methods set of methods to add to
     * @param forbidden set of forbidden methods
     * @param newMethod method to add
     * @param flags method modifiers
     */
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

    /**
     * Creates methods given as {@link Stream}
     * @param methods methods to add to implementation
     */
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

    /**
     * Checks if {@link Executable}
     * ({@link Method} or {@link Constructor}) is forbidden to implement
     * @param flags modifiers of executable
     * @return true if executable is forbidden, else false
     */
    private static boolean forbiddenExecutable(int flags) {
        return Modifier.isFinal(flags)
                || Modifier.isStatic(flags)
                || Modifier.isNative(flags)
                || Modifier.isPrivate(flags);
    }

    /**
     * Creates one full type parameter with bounds relatively to declaring class
     *
     * @param name of the variable to work with
     * @param word extends or super
     * @param bounds type's bounds
     * @param declaringClass class, in which variable was declared
     * @return full type parameter: T extends/super E1 & E2 ...
     */
    private String getFullTypeDeclaration(
            final String name,
            final String word,
            final Type[] bounds,
            final Class<?> declaringClass
    ) {
        return name + (bounds.length == 1 && bounds[0] == Object.class ? ""
                : " " + word + " " + join(Arrays.stream(bounds).map(t -> getType(t, declaringClass)), " & "));
    }

    /**
     * Creates one full type parameter with upper bounds relatively to declaring class
     *
     * @param name of the variable to work with
     * @param bounds type's bounds
     * @param declaringClass class, in which variable was declared
     * @return full type parameter: T extends E1 & E2 ...
     */
    private String getFullTypeDeclarationUpper(final String name, final Type[] bounds, final Class<?> declaringClass) {
        return getFullTypeDeclaration(name, "extends", bounds, declaringClass);
    }

    /**
     * Creates one full wildcard relatively to declaring class
     *
     * @param bounds type's bounds
     * @param declaringClass class, in which wildcard was declared
     * @return full type parameter: ? super E1 & E2 ...
     */
    private String getFullTypeDeclarationLower(final Type[] bounds, final Class<?> declaringClass) {
        return getFullTypeDeclaration("?", "super", bounds, declaringClass);
    }

    /**
     * Returns actual generic type (with all substitutions)
     *
     * @param type type to work with
     * @param declaringClass class, in which type was declared
     * @return String representation of the actual type
     */
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

    /**
     * Substitutes type from typesMap
     *
     * @param type type to work with
     * @param declaringClass class, in which type was declared
     * @return String representation of the actual type
     */
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

    /**
     * Gets full member class name if it is not private
     * @param clazz class to get name
     * @return full member class name
     * @throws UncheckedImplerException if inner class at some level is private
     */
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
            outputStream.write(builder.toString());
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
        return getImplClassFullName(clazz, ".java");
    }

    private static String getImplClassFullName(final Class<?> clazz, final String extension) {
        return (clazz.getPackageName() + "." + clazz.getSimpleName()).replace('.', '\\') + "Impl" + extension;
    }

    private static Path createOutputFile(final Path root, final String classFullName) throws IOException {
        if (root == null) {
            throw new IOException("Root cannot be null");
        }
        return createFile(Path.of(root.toString(), classFullName));
    }

    private static Path createFile(final Path file) throws IOException {
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

    private static class ImplementorOutputStream implements AutoCloseable {
        private final OutputStream stream;
        private final Path path;

        private ImplementorOutputStream(final OutputStream stream, final Path path) {
            this.stream = stream;
            this.path = path;
        }

        public Path getPath() {
            return path;
        }

        private void write(final String string) throws IOException {
            stream.write(string.getBytes(StandardCharsets.UTF_8));
        }

        private void write(final StringBuilder builder) throws IOException {
            write(builder.toString());
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    @FunctionalInterface
    private interface OutputStreamGetter {
        ImplementorOutputStream getStream(Class<?> clazz, Path path) throws IOException;
    }

    /* JAR IMPLEMENTOR */

    public static void main(final String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong Number of arguments");
            return;
        }
        try {
            Implementor implementor = new Implementor();
            Class<?> clazz = getClass(args[0]);
            Path path = getPath(args[1]);
            implementor.implementJar(clazz, path);
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

    @Override
    public void implementJar(final Class<?> clazz, final Path jarFile) throws ImplerException {
        implement(clazz, TEMP_ROOT, Implementor::getBufferedOutputStream);
        try (JarOutputStream jarOutputStream = getJarOutputStream(clazz, jarFile)) {
            String javaTempFile = outputStream.getPath().toString();
            compile(javaTempFile);
            copyToJar(jarOutputStream, javaTempFile);
            jarOutputStream.closeEntry();
            clean();
        } catch (IOException e) {
            try {
                Files.delete(jarFile);
            } catch (IOException e2) {
                throw new ImplerException("Unable to delete jar file after error occurred:\n" + e2.getMessage());
            }
            throw new ImplerException("Unable to create jar file due to error: " + e.getMessage());
        }
    }

    private static JarOutputStream getJarOutputStream(final Class<?> clazz, final Path jarPath) throws IOException {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarPath.toFile()));
        jarOutputStream.putNextEntry(
                new JarEntry(getImplClassFullName(clazz, ".class").replace("\\", "/"))
        );
        return jarOutputStream;
    }

    private static void compile(final String path) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int exitCode = compiler.run(null, null, null, path);
        if (exitCode != 0) {
            throw new ImplerException("Unable to compile temp jar class");
        }
    }

    private static void copyToJar(final JarOutputStream jarOutputStream, final String javaFile) throws ImplerException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(toClassFile(javaFile)))) {
            inputStream.transferTo(jarOutputStream);
        } catch (IOException e) {
            throw new ImplerException("Unable to copy temp .class file to jar: " + e.getMessage());
        }
    }

    private static String toClassFile(final String javaFile) {
        return javaFile.substring(0, javaFile.length() - 5) + ".class";
    }

    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    private static final Path TEMP_ROOT = Path.of("temp");

    private static void clean() throws IOException {
        Files.walkFileTree(TEMP_ROOT, DELETE_VISITOR);
    }
}
