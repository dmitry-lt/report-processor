package report.processor.impl;

import org.junit.jupiter.api.Test;
import report.processor.FileInfo;
import report.processor.ReportHandler;
import report.processor.ReportProcessor;
import report.processor.impl.ExecutorReportProcessor;
import report.processor.impl.FileSystemHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class ExecutorReportProcessorIntegrationTests {
    private final int handlerTimeoutMillis = 1000;
    private final int terminationTimeoutMillis = 1000;

    private Path folderPath() {
        return Paths.get("test_folder");
    }

    private Path filePath(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".xml");
    }

    private Path filePathNonXml(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".txt");
    }

    private Path filePathXML(Path folderPath) {
        return folderPath.resolve(UUID.randomUUID() + ".XML");
    }

    private Path filePath(Path folderPath, String fileName) {
        return folderPath.resolve(fileName + ".xml");
    }

    private ReportProcessor reportProcessor(FakeFileSystemHelper fileInfoProvider) {
        return new ExecutorReportProcessor(fileInfoProvider, filePath -> filePath.toString().toLowerCase().endsWith(".xml"), 10);
    }

    private FakeFileSystemHelper fileInfoProvider() {
        return new FakeFileSystemHelper();
    }

    class FakeFileSystemHelper implements FileSystemHelper {
        public ArrayList<FileInfo> fileInfos = new ArrayList<>();

        @Override
        public Collection<FileInfo> getFileInfos(Path folder) {
            return fileInfos;
        }
    }

    @Test
    public void givenProcessor_whenCreateFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePath(folderPath);
        var reportType = "reportType";
        var fileInfoProvider = fileInfoProvider();
        var reportProcessor = reportProcessor(fileInfoProvider);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileInfoProvider.fileInfos.add(new FileInfo(filePath, FileTime.from(Instant.now()), FileTime.from(Instant.now())));

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessor_whenCreateXMLFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePathXML(folderPath);
        var reportType = "reportType";
        var fileInfoProvider = fileInfoProvider();
        var reportProcessor = reportProcessor(fileInfoProvider);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileInfoProvider.fileInfos.add(new FileInfo(filePath, FileTime.from(Instant.now()), FileTime.from(Instant.now())));

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessor_whenCreateNonXmlFile_thenDontHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePathNonXml(folderPath);
        var reportType = "reportType";
        var fileInfoProvider = fileInfoProvider();
        var reportProcessor = reportProcessor(fileInfoProvider);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileInfoProvider.fileInfos.add(new FileInfo(filePath, FileTime.from(Instant.now()), FileTime.from(Instant.now())));

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, never()).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessor_whenUnregisterHandler_thenStopHandlingFiles() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePath(folderPath);
        var filePath2 = filePath(folderPath);
        var reportType = "reportType";
        var fileInfoProvider = fileInfoProvider();
        var reportProcessor = reportProcessor(fileInfoProvider);
        var handler = mock(ReportHandler.class);
        var handler2 = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handlers
        reportProcessor.registerHandler(handler, reportType);
        reportProcessor.registerHandler(handler2, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileInfoProvider.fileInfos.add(new FileInfo(filePath, FileTime.from(Instant.now()), FileTime.from(Instant.now())));
        // verify file modified event
        verify(handler, timeout(handlerTimeoutMillis)).handle(argThat(e -> e.filePath().equals(filePath)));
        verify(handler2, timeout(handlerTimeoutMillis)).handle(argThat(e -> e.filePath().equals(filePath)));
        // unregister second handler
        reportProcessor.unregisterHandler(handler2);
        // create second XML file
        fileInfoProvider.fileInfos.add(new FileInfo(filePath2, FileTime.from(Instant.now()), FileTime.from(Instant.now())));

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify second file not handled by second handler
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
        verify(handler2, never()).handle(argThat(e -> e.filePath().equals(filePath2)));
    }

    @Test
    public void givenProcessor_whenShutdownGracefully_thenHandleAllExistingFiles() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var reportType = "reportType";
        var fileInfoProvider = fileInfoProvider();
        var reportProcessor = reportProcessor(fileInfoProvider);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);

        // make handler busy processing the first file
        var latch = new CountDownLatch(1);

        var processedFilesCount = new AtomicInteger();
        var handler = mock(ReportHandler.class);
        doAnswer(invocation -> {
            latch.await();
            processedFilesCount.incrementAndGet();
            return null;
        }).when(handler).handle(any());

        // register handler
        reportProcessor.registerHandler(handler, reportType);

        // start
        reportProcessor.start();
        // create XML files
        int numberOfFiles = 10;
        for (int i = 0; i < numberOfFiles; i++) {
            var filePath = filePath(folderPath, String.valueOf(i));
            fileInfoProvider.fileInfos.add(new FileInfo(filePath, FileTime.from(Instant.now()), FileTime.from(Instant.now())));
        }

        // finish processing the first file
        latch.countDown();

        // then
        // shutdown gracefully
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify that all files were handled by all handlers
        assertEquals(numberOfFiles, processedFilesCount.get());
    }

    @Test
    public void givenProcessor_whenShutdownForced_thenDontNecessarilyHandleAllExistingFiles() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var reportType = "reportType";
        var fileInfoProvider = fileInfoProvider();
        var reportProcessor = reportProcessor(fileInfoProvider);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);

        // make handler busy processing the first file
        var latch = new CountDownLatch(1);

        var processedFilesCount = new AtomicInteger();
        var handler = mock(ReportHandler.class);
        doAnswer(invocation -> {
            latch.await();
            processedFilesCount.incrementAndGet();
            return null;
        }).when(handler).handle(any());

        // register handler
        reportProcessor.registerHandler(handler, reportType);

        // start
        reportProcessor.start();
        // create XML files
        int numberOfFiles = 10;
        for (int i = 0; i < numberOfFiles; i++) {
            var filePath = filePath(folderPath, String.valueOf(i));
            fileInfoProvider.fileInfos.add(new FileInfo(filePath, FileTime.from(Instant.now()), FileTime.from(Instant.now())));
        }

        // finish processing the first file
        latch.countDown();

        // then
        // force shutdown
        reportProcessor.shutdownNow();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify that not all files were handled by all handlers
        assertTrue(processedFilesCount.get() < numberOfFiles);
    }
}
