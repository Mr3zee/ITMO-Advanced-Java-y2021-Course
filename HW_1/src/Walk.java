import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.stream.Stream;

public class Walk {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.format("Wrong number of arguments: %d", args.length);
            return;
        }
        start(args[0], args[1]);
    }

    private static void start(String inputPath, String outputPath) {
        Path input = Path.of(inputPath);
        Path output = Path.of(outputPath);
        ArrayList<String> list = new ArrayList<>();
        try (Stream<String> lines = Files.lines(input, StandardCharsets.UTF_8)) {
            createIfNotExists(output);
            lines.forEach(line -> list.add(prettyFormat(processFile(Path.of(line)))));
            Files.write(output, list, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.format("I/O Exception caught:\n%s: %s\n", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static void createIfNotExists(Path file) throws IOException {
        if (!Files.exists(file)) {
            Path parent = file.getParent();
            if (parent == null) throw new IOException("Parent is null for output");
            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(file);
        }
    }

    private static final int byteSize = 1024;

    private static FileInfo processFile(Path file) {
        long hash = 0;
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] bytes = new byte[byteSize];
            int bytesRead;
            while ((bytesRead = stream.readNBytes(bytes, 0, byteSize)) > 0) {
                hash = Hash.pjw(bytes, bytesRead, hash);
            }
        } catch (IOException ignored) {}
        return new FileInfo(hash, file.toString());
    }

    private static String prettyFormat(FileInfo file) {
        return String.format("%016x %s", file.hash, file.name);
    }

    private static class FileInfo {
        public long hash;
        public String name;

        public FileInfo(long hash, String name) {
            this.hash = hash;
            this.name = name;
        }
    }
}
