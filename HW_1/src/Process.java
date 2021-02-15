import exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class Process {

    public static void safeStart(String[] args, boolean withDirs) {
        try {
            start(args, withDirs);
        } catch (WalkException e) {
            e.print();
        }
    }

    public static void start(String[] args, boolean withDirs) throws WalkException {
        if (args.length != 2) {
            WalkException.create(WalkException.class,
                    "IllegalArgument", String.format("Wrong number of arguments: %d, required: 2", args.length)
            ).print();
            return;
        }

        Path out = Path.of(args[1]);
        Path in = Path.of(args[0]);
        checkSameFile(in, out);
        initOutputFile(out);

        try (Stream<String> lines = Files.lines(in, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                Path path = Path.of(line);
                if (Files.isDirectory(path)) {
                    if (withDirs) {
                        processDir(path, out);
                    } else {
                        writeln(prettyFormat(0, path), out);
                    }
                } else {
                    processFile(path, out);
                }
            });
        } catch (IOException e) {
            throw WalkException.create(WalkInputException.class, e, in);
        }
    }

    private static void checkSameFile(Path in, Path out) throws WalkException {
        try {
            if (Files.isSameFile(in, out)) {
                throw new IOException("input and output files can not be the same one");
            }
        } catch (IOException e) {
            throw WalkException.create(WalkException.class, e, in, out);
        }
    }

    private static void processDir(Path dir, Path out) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    processFile(file, out);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    WalkException.create(WalkFileVisitException.class, exc, dir, file).print();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            WalkException.create(WalkDirException.class, e, dir).print();
        }
    }

    private static void processFile(Path file, Path out) {
        String outputString = prettyFormat(calcFileHash(file), file);
        writeln(outputString, out);
    }

    private static void writeln(String string, Path out) {
        try {
            write(string, out);
            write("\n", out);
        } catch (IOException e) {
            WalkException.create(WalkWriteOutputException.class, e, out).print();
        }
    }

    private static void write(String string, Path out) throws IOException {
        Files.writeString(out, string, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static final int bytesSize = 1024;

    private static long calcFileHash(Path file) {
        long hash = 0;
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] bytes = new byte[bytesSize];
            int bytesRead;
            while ((bytesRead = stream.readNBytes(bytes, 0, bytesSize)) > 0) {
                hash = Hash.pjw(bytes, bytesRead, hash);
            }
        } catch (IOException e) {
            WalkException.create(WalkHashException.class, e, file).print();
        }
        return hash;
    }

    private static String prettyFormat(long hash, Path name) {
        return String.format("%016x %s", hash, name.toString());
    }

    private static void initOutputFile(Path file) throws WalkException {
        try {
            if (!Files.exists(file)) {
                Path parent = file.getParent();
                if (parent == null) throw new IOException("File's parent do not exist");
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(file);
            } else {
                Files.writeString(file, "", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw WalkException.create(WalkCreateOutputException.class, e, file);
        }
    }
}
