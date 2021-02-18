import exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class Process {

    public static void safeStart(final String[] args, final boolean withDirs) {
        try {
            start(args, withDirs);
        } catch (WalkException e) {
            e.print();
        }
    }

    public static void start(final String[] args, final boolean withDirs) throws WalkException {
        try {
            if (args == null) {
                throw new IllegalArgumentException("args can not be null");
            }
            if (args.length != 2) {
                throw new IllegalArgumentException(String.format("Wrong number of arguments: %d, required: 2", args.length));
            }
            if (args[0] == null || args[1] == null) {
                throw new IllegalArgumentException("input and output file cannot be null");
            }
        } catch (IllegalArgumentException e) {
            throw WalkException.create(WalkException.class, e);
        }

        final Path out, in;

        try {
            out = Path.of(args[1]);
            in = Path.of(args[0]);
        } catch (InvalidPathException e) {
            throw WalkException.create(WalkFormatException.class, e);
        }
        checkSameFile(in, out);
        initOutputFile(out);

        try (Stream<String> lines = Files.lines(in, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                final Path path;
                try {
                    path = Path.of(line);
                } catch (InvalidPathException e) {
                    writeError(line, out);
                    WalkException.create(WalkFormatException.class, e).print();
                    return;
                }
                if (Files.isDirectory(path)) {
                    if (withDirs) {
                        processDir(path, out);
                    } else {
                        writeError(path.toString(), out);
                    }
                } else {
                    processFile(path, out);
                }
            });
        } catch (IOException e) {
            throw WalkException.create(WalkInputException.class, e, in.toString());
        }
    }

    private static void checkSameFile(final Path in, final Path out) throws WalkException {
        try {
            if (Files.exists(in) && Files.exists(out) && Files.isSameFile(in, out)) {
                throw new IOException("input and output files can not be the same one");
            }
        } catch (IOException e) {
            throw WalkException.create(WalkException.class, e, in.toString(), out.toString());
        }
    }

    private static void processDir(final Path dir, final Path out) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    processFile(file, out);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    WalkException.create(WalkFileVisitException.class, exc, dir.toString(), file.toString()).print();
                    writeError(file.toString(), out);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            WalkException.create(WalkDirException.class, e, dir.toString()).print();
        }
    }

    private static void processFile(final Path file, final Path out) {
        final String outputString = prettyFormat(calcFileHash(file), file.toString());
        writeln(outputString, out);
    }

    private static void writeError(final String string, final Path out) {
        writeln(prettyFormat(0, string), out);
    }

    private static void writeln(final String string, final Path out) {
        try {
            write(String.format("%s%n", string), out);
        } catch (IOException e) {
            WalkException.create(WalkWriteOutputException.class, e, out.toString()).print();
        }
    }

    private static void write(final String string, final Path out) throws IOException {
        Files.writeString(out, string, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
    // TODO: 15.02.2021 make buffered

    private static final int bytesSize = 1024;

    private static long calcFileHash(final Path file) {
        long hash = 0;
        try (InputStream stream = Files.newInputStream(file)) {
            final byte[] bytes = new byte[bytesSize];
            int bytesRead;
            while ((bytesRead = stream.readNBytes(bytes, 0, bytesSize)) > 0) {
                hash = Hash.pjw(bytes, bytesRead, hash);
            }
        } catch (IOException e) {
            WalkException.create(WalkHashException.class, e, file.toString()).print();
        }
        return hash;
    }

    private static String prettyFormat(final long hash, final String name) {
        return String.format("%016x %s", hash, name);
    }

    private static void initOutputFile(final Path file) throws WalkException {
        try {
            if (!Files.exists(file)) {
                final Path parent = file.getParent();
                if (parent == null) throw new IOException("File's parent do not exist");
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(file);
            } else {
                Files.writeString(file, "", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw WalkException.create(WalkCreateOutputException.class, e, file.toString());
        }
    }
}
