package report.processor.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class TestUtils {
    public static final Path TEMP_FOLDER = Paths.get(System.getProperty("java.io.tmpdir"));

    public static void createFolder(Path folderPath) {
        try {
            Files.createDirectories(folderPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteFolder(Path folderPath) {
        try {
            Files.walk(folderPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createFile(Path filePath) {
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createFile(Path filePath, String content) {
        try {
            Files.createFile(filePath);
            var writer = new BufferedWriter(new FileWriter(filePath.toFile()));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void modifyFile(Path filePath, String content) {
        try {
            var writer = new BufferedWriter(new FileWriter(filePath.toFile()));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
