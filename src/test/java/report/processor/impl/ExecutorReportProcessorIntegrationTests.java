package report.processor.impl;

import org.junit.jupiter.api.Test;
import report.processor.FileInfo;
import report.processor.ReportHandler;
import report.processor.ReportProcessor;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.time.Instant.now;
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
import static report.processor.util.TestUtils.filePath;
import static report.processor.util.TestUtils.filePathNonXml;
import static report.processor.util.TestUtils.filePathXML;
import static report.processor.util.TestUtils.folderPath;

public class ExecutorReportProcessorIntegrationTests {
    private final int handlerTimeoutMillis = 1000;
    private final int terminationTimeoutMillis = 1000;

    private ReportProcessor reportProcessor(FakeFileSystemHelper fileSystemHelper) {
        return new ExecutorReportProcessor(fileSystemHelper, filePath -> filePath.toString().toLowerCase().endsWith(".xml"), 10);
    }

    private FakeFileSystemHelper fileSystemHelper() {
        return new FakeFileSystemHelper();
    }

    class FakeFileSystemHelper implements FileSystemHelper {
        public ArrayList<FileInfo> fileInfos = new ArrayList<>();

        @Override
        public Collection<FileInfo> getFileInfos(Path folder, Predicate<Path> filePathFilter) {
            return fileInfos;
        }
    }

    @Test
    public void givenProcessor_whenCreateFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePath(folderPath);
        var reportType = "reportType";
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event
        verify(handler, atLeast(1)).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessor_whenHandlerRegisteredForMultipleReportTypes_thenHandleFileOnlyOnce() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePath(folderPath);
        var reportType1 = "reportType1";
        var reportType2 = "reportType2";
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType1, reportType2);
        // register handler
        reportProcessor.registerHandler(handler, reportType1, reportType2);
        // start
        reportProcessor.start();
        // create XML file
        fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));

        // then
        // stop
        reportProcessor.shutdown();
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify file modified event was handled by the handler exactly once
        verify(handler).handle(argThat(e -> e.filePath().equals(filePath)));
    }

    @Test
    public void givenProcessor_whenCreateXMLFile_thenHandleFile() throws InterruptedException {
        // given
        var folderPath = folderPath();
        var filePath = filePathXML(folderPath);
        var reportType = "reportType";
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));

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
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);
        var handler = mock(ReportHandler.class);

        // when
        // add folder
        reportProcessor.addMonitoredFolder(folderPath, reportType);
        // register handler
        reportProcessor.registerHandler(handler, reportType);
        // start
        reportProcessor.start();
        // create XML file
        fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));

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
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);
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
        fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));
        // verify file modified event
        verify(handler, timeout(handlerTimeoutMillis)).handle(argThat(e -> e.filePath().equals(filePath)));
        verify(handler2, timeout(handlerTimeoutMillis)).handle(argThat(e -> e.filePath().equals(filePath)));
        // unregister second handler
        reportProcessor.unregisterHandler(handler2);
        // create second XML file
        fileSystemHelper.fileInfos.add(new FileInfo(filePath2, FileTime.from(now()), FileTime.from(now())));

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
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);

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
            fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));
        }

        // finish processing the first file
        latch.countDown();

        // then
        // shutdown gracefully
        reportProcessor.shutdown();
        // verify that not all files have been handled by all handlers yet
        assertTrue(numberOfFiles > processedFilesCount.get());
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
        var fileSystemHelper = fileSystemHelper();
        var reportProcessor = reportProcessor(fileSystemHelper);

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
            fileSystemHelper.fileInfos.add(new FileInfo(filePath, FileTime.from(now()), FileTime.from(now())));
        }

        // finish processing the first file
        latch.countDown();

        // then
        // force shutdown
        reportProcessor.shutdownNow();
        var processFilesCountAfterShutdown = processedFilesCount.get();
        // verify that not all files were handled by all handlers
        assertTrue(processFilesCountAfterShutdown < numberOfFiles);
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));
        // verify that no files were handled from forced shutdown until termination
        assertEquals(processFilesCountAfterShutdown, processedFilesCount.get());
    }
}
