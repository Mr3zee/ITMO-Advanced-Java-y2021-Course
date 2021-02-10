import exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Process {

    public static void safeStart(String[] args) {
        try {
            start(args);
        } catch (WalkException e) {
            e.print();
        }
    }

    public static void start(String[] args) throws WalkException {
        if (args.length != 2) {
            System.out.format("Wrong number of arguments: %d, required: 2", args.length);
            return;
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        ArrayList<String> list = new ArrayList<>();
        try (Stream<String> lines = Files.lines(input, StandardCharsets.UTF_8)) {
            createIfNotExists(output);
            lines.forEach(line -> {
                Path path = Path.of(line);
                (Files.isDirectory(path) ? processDir(path): List.of(path)).forEach(file -> list.add(processFile(file)));
            });
            // TODO: 11.02.2021 write after read
            try {
                Files.write(output, list, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw WalkException.create(WalkWriteOutputException.class, output, e);
            }
        } catch (IOException e) {
            throw WalkException.create(WalkInputException.class, input, e);
        }
    }

    private static List<Path> processDir(Path dir) {
        List<Path> list = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    list.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    WalkException.create(WalkFileVisitException.class, file, exc).print();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            WalkException.create(WalkDirException.class, dir, e).print();
        }
        return list;
    }

    private static String processFile(Path file) {
        return prettyFormat(calcFileHash(file), file.toString());
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
            WalkException.create(WalkHashException.class, file, e).print();
        }
        return hash;
    }

    private static String prettyFormat(long hash, String name) {
        return String.format("%016x %s", hash, name);
    }

    private static void createIfNotExists(Path file) throws WalkException {
        if (!Files.exists(file)) {
            try {
                Path parent = file.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(file);
            } catch (IOException e) {
                throw WalkException.create(WalkCreateOutputException.class, file, e);
            }
        }
    }
}
