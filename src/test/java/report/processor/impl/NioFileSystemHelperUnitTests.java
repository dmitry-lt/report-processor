package report.processor.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static report.processor.util.TestUtils.createFile;
import static report.processor.util.TestUtils.createFolder;
import static report.processor.util.TestUtils.deleteFolder;
import static report.processor.util.TestUtils.filePath;
import static report.processor.util.TestUtils.filePathNonXml;
import static report.processor.util.TestUtils.folderPath;
import static report.processor.util.TestUtils.modifyFile;

public class NioFileSystemHelperUnitTests {
    private static final Path testFolder = folderPath();

    @BeforeAll
    public static void setUp() {
        createFolder(testFolder);
    }

    @AfterAll
    public static void tearDown() {
        deleteFolder(testFolder);
    }

    private String fileContent() {
        return "<?xml version=\"1.0\"?><report></report>";
    }

    private String fileContentModified() {
        return "<?xml version=\"1.0\"?><report2></report2>";
    }

    private FileSystemHelper fileSystemHelper() {
        return new NioFileSystemHelper();
    }

    private Predicate<Path> xmlPathPredicate() {
        return filePath -> filePath.toString().toLowerCase().endsWith(".xml");
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
    public void givenFileSystemHelper_whenCreateFile_thenGetFileInfo() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var filePath = filePath(folderPath);
        var fileContent = fileContent();
        var provider = fileSystemHelper();

        // when
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);

        // then
        var fileInfo = provider.getFileInfos(folderPath, xmlPathPredicate()).iterator().next();
        // verify path, creation and modification time
        assertEquals(filePath, fileInfo.filePath());
        assertEquals(getFileCreationTime(filePath), fileInfo.creationTime());
        assertEquals(getFileModificationTime(filePath), fileInfo.modificationTime());
    }

    @Test
    public void givenFileSystemHelper_whenModifyFile_thenGetFileInfo() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var filePath = filePath(folderPath);
        var fileContent = fileContent();
        var provider = fileSystemHelper();

        // when
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);
        // modify XML file
        modifyFile(filePath, fileContentModified());

        // then
        var fileInfo = provider.getFileInfos(folderPath, xmlPathPredicate()).iterator().next();
        // verify path, creation and modification time
        assertEquals(filePath, fileInfo.filePath());
        assertEquals(getFileCreationTime(filePath), fileInfo.creationTime());
        assertEquals(getFileModificationTime(filePath), fileInfo.modificationTime());
    }

    @Test
    public void givenFileSystemHelper_whenCreateNonXmlFile_thenDontGetFileInfo() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var filePath = filePathNonXml(folderPath);
        var fileContent = fileContent();
        var provider = fileSystemHelper();

        // when
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);

        // then
        // verify empty result
        assertEquals(0, provider.getFileInfos(folderPath, xmlPathPredicate()).size());
    }

    @Test
    public void givenFileSystemHelper_whenModifyNonXmlFile_thenDontGetFileInfo() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var filePath = filePathNonXml(folderPath);
        var fileContent = fileContent();
        var provider = fileSystemHelper();

        // when
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);
        // modify XML file
        modifyFile(filePath, fileContentModified());

        // then
        // verify empty result
        assertEquals(0, provider.getFileInfos(folderPath, xmlPathPredicate()).size());
    }
}
