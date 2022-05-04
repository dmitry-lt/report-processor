package report.processor.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.UUID;

public class TestUtils {
    private static final Path TEMP_FOLDER = Paths.get(System.getProperty("java.io.tmpdir"));

    public static Path folderPath() {
        return TEMP_FOLDER.resolve(UUID.randomUUID().toString());
    }

    public static Path folderPath(Path parentFolderPath) {
        return parentFolderPath.resolve(UUID.randomUUID().toString());
    }

    public static Path filePath(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".xml");
    }

    public static Path filePath(Path folderPath, String fileName) {
        return folderPath.resolve(fileName + ".xml");
    }

    public static Path filePathNonXml(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".txt");
    }

    public static Path filePathXML(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".XML");
    }

    public static void createFolder(Path folderPath) {
        try {
            Files.createDirectories(folderPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteFolder(Path folderPath) {
        try (var fileStream = Files.walk(folderPath)) {
            fileStream
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

    public static FileTime getFileCreationTime(Path filePath) {
        try {
            var attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            return attr.creationTime();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
