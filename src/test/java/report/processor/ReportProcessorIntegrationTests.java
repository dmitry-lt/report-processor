package report.processor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import static java.time.Instant.EPOCH;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static report.processor.util.TestUtils.createFile;
import static report.processor.util.TestUtils.createFolder;
import static report.processor.util.TestUtils.deleteFolder;
import static report.processor.util.TestUtils.filePath;
import static report.processor.util.TestUtils.folderPath;
import static report.processor.util.TestUtils.getFileCreationTime;

public class ReportProcessorIntegrationTests {
    private static final Path testFolder = folderPath();

    @BeforeAll
    public static void setUp() {
        createFolder(testFolder);
    }

    @AfterAll
    public static void tearDown() {
        deleteFolder(testFolder);
    }

    private final int terminationTimeoutMillis = 1000;

    private String fileContent() {
        return "<?xml version=\"1.0\"?><report></report>";
    }

    private ReportProcessor reportProcessor() {
        return ReportProcessors.newXmlReportProcessor(10);
    }

    @Test
    public void givenProcessor_whenCreateFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var filePath = filePath(folderPath);
        var fileContent = fileContent();
        var reportType = "reportType";
        var reportProcessor = reportProcessor();
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessorAndMultipleHandlers_whenCreateFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPathA = folderPath(testFolder);
        var filePathA = filePath(folderPathA, "a");
        var folderPathB = folderPath(testFolder);
        var filePathB1 = filePath(folderPathB, "b1");
        var filePathB2 = filePath(folderPathB, "b2");
        var fileContent = fileContent();
        var reportType = "reportType";
        var reportProcessor = reportProcessor();
        var handler = mock(ReportHandler.class);

        // when
        // add folders
        reportProcessor.addMonitoredFolder(folderPathA, reportType);
        reportProcessor.addMonitoredFolder(folderPathB, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create folders
        createFolder(folderPathA);
        createFolder(folderPathB);
        // create XML files
        createFile(filePathA, fileContent);
        createFile(filePathB1, fileContent);
        createFile(filePathB2, fileContent);

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePathA)));
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePathB1)));
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePathB2)));
    }

    @Test
    public void givenProcessorAndMultipleFoldersAndFiles_whenCreateFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var filePath = filePath(folderPath);
        var fileContent = fileContent();
        var reportType = "reportType";
        var reportProcessor = reportProcessor();
        var handler = mock(ReportHandler.class);
        var handler2 = mock(ReportHandler.class);
        var handler3 = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        reportProcessor.registerHandler(handler2, reportType);
        reportProcessor.registerHandler(handler3, reportType);
        // start
        reportProcessor.start();
        // create folder
        createFolder(folderPath);
        // create XML file
        createFile(filePath, fileContent);

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
        verify(handler2, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
        verify(handler3, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessor_whenRemoveFolder_thenHandleExistingAndDontHandleLaterAddedFiles() throws InterruptedException {
        // given
        var folderPathA = folderPath(testFolder);
        var filePathA1 = filePath(folderPathA, "a1");
        var folderPathB = folderPath(testFolder);
        var filePathB1 = filePath(folderPathB, "b1");
        var filePathB2 = filePath(folderPathB, "b2");
        var fileContent = fileContent();
        var reportType = "reportType";
        var reportProcessor = reportProcessor();
        var handler = mock(ReportHandler.class);

        // when
        // start
        reportProcessor.start();
        // create folders
        createFolder(folderPathA);
        createFolder(folderPathB);
        // add folders
        reportProcessor.addMonitoredFolder(folderPathA, reportType);
        reportProcessor.addMonitoredFolder(folderPathB, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // create XML files
        createFile(filePathA1, fileContent);
        createFile(filePathB1, fileContent);
        // remove folder
        reportProcessor.removeMonitoredFolder(folderPathA);

        // create a file in folder A after the folder has been removed from monitoring
        var folderARemovedTime = now();
        var fileCreationTime = EPOCH;
        Path filePathANotToBeHandled = null;
        while (fileCreationTime.compareTo(folderARemovedTime) <= 0) {
            filePathANotToBeHandled = filePath(folderPathA);
            createFile(filePathANotToBeHandled, fileContent);
            fileCreationTime = getFileCreationTime(filePathANotToBeHandled).toInstant();
        }
        var filePathA2 = filePathANotToBeHandled;
        // create a file in folder B
        createFile(filePathB2, fileContent);

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified events
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePathA1)));
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePathB1)));
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePathB2)));
        // verify that files in removed folder are not handled anymore
        verify(handler, never()).handle(argThat(e -> e.filePath().equals(filePathA2)));
    }

    @Test
    public void givenProcessor_whenShutdownGracefully_thenHandleAllExistingFiles() throws InterruptedException {
        // given
        var folderPath = folderPath(testFolder);
        var numberOfFiles = 5;
        var fileContent = fileContent();
        var reportType = "reportType";
        var reportProcessor = reportProcessor();
        var handler1 = mock(ReportHandler.class);
        var handler2 = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handlers
        reportProcessor.registerHandler(handler1, reportType);
        reportProcessor.registerHandler(handler2, reportType);
        // start
        reportProcessor.start();
        // create folder
        createFolder(folderPath);

        // make handlers busy processing the first file
        var latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.await();
            return null;
        }).when(handler1).handle(any());
        doAnswer(invocation -> {
            latch.await();
            return null;
        }).when(handler2).handle(any());

        // create XML files
        for (int i = 0; i < numberOfFiles; i++) {
            createFile(filePath(folderPath, String.valueOf(i)), fileContent);
        }

        // finish processing the first file
        latch.countDown();

        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));

        // then
        // verify that all files were handled by all handlers
        for (int i = 0; i < numberOfFiles; i++) {
            var iString = String.valueOf(i);
            verify(handler1, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath(folderPath, iString))));
            verify(handler2, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath(folderPath, iString))));
        }
    }

}
