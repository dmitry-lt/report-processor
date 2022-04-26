package report.processor.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import report.processor.impl.FileSystemHelper;
import report.processor.impl.NioFileSystemHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static report.processor.util.TestUtils.TEMP_FOLDER;
import static report.processor.util.TestUtils.createFile;
import static report.processor.util.TestUtils.createFolder;
import static report.processor.util.TestUtils.deleteFolder;
import static report.processor.util.TestUtils.modifyFile;

public class NioFileSystemHelperUnitTests {
    private static final Path testFolder = TEMP_FOLDER.resolve(UUID.randomUUID().toString());

    @BeforeAll
    public static void setUp() {
        createFolder(testFolder);
    }

    @AfterAll
    public static void tearDown() {
        deleteFolder(testFolder);
    }

    private Path folderPath() {
        return testFolder.resolve(UUID.randomUUID().toString());
    }

    private Path filePath(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".xml");
    }

    private Path filePath(Path folderPath, String fileName) {
        return folderPath.resolve(fileName + ".xml");
    }

    private String fileContent() {
        return "<?xml version=\"1.0\"?><report></report>";
    }

    private String fileContentModified() {
        return "<?xml version=\"1.0\"?><report2></report2>";
    }

    private FileSystemHelper fileInfoProvider() {
        return new NioFileSystemHelper();
    }

    private FileTime getFileCreationTime(Path path) {
        try {
            var attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.creationTime();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileTime getFileModificationTime(Path path) {
        try {
            var attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.lastModifiedTime();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void givenProvider_whenCreateFile_thenGetFileInfo() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePath(folderPath);
        var fileContent = fileContent();
        var provider = fileInfoProvider();

        // when
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);

        // then
        var fileInfo = provider.getFileInfos(folderPath).iterator().next();
        // verify path, creation and modification time
        assertEquals(filePath, fileInfo.filePath());
        assertEquals(getFileCreationTime(filePath), fileInfo.creationTime());
        assertEquals(getFileModificationTime(filePath), fileInfo.modificationTime());
    }

    @Test
    public void givenProvider_whenModifyFile_thenGetFileInfo() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePath(folderPath);
        var fileContent = fileContent();
        var provider = fileInfoProvider();

        // when
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);
        // modify XML file
        modifyFile(filePath, fileContentModified());

        // then
        var fileInfo = provider.getFileInfos(folderPath).iterator().next();
        // verify path, creation and modification time
        assertEquals(filePath, fileInfo.filePath());
        assertEquals(getFileCreationTime(filePath), fileInfo.creationTime());
        assertEquals(getFileModificationTime(filePath), fileInfo.modificationTime());
    }
}
