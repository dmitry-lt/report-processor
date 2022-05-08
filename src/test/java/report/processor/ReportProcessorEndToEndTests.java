package report.processor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static report.processor.util.TestUtils.createFile;
import static report.processor.util.TestUtils.createFolder;
import static report.processor.util.TestUtils.deleteFolder;
import static report.processor.util.TestUtils.filePath;
import static report.processor.util.TestUtils.folderPath;

public class ReportProcessorEndToEndTests {
    private static final Path testFolder = folderPath();

    @BeforeAll
    public static void setUp() {
        createFolder(testFolder);
    }

    @AfterAll
    public static void tearDown() {
        deleteFolder(testFolder);
    }

    private final int terminationTimeoutMillis = 10_000;

    private String fileContent1() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="report.processor.ReportProcessorIntegrationTests" tests="5" skipped="0" failures="0" errors="0" timestamp="2022-04-25T19:47:41" hostname="hostname" time="0.113">
                  <properties/>
                  <testcase name="givenProcessor_whenCreateFile_thenHandleFile()" classname="report.processor.ReportProcessorIntegrationTests" time="0.01"/>
                  <testcase name="givenProcessor_whenRemoveFolder_thenHandleExistingDontHandleLaterAddedFiles()" classname="report.processor.ReportProcessorIntegrationTests" time="0.03"/>
                  <testcase name="givenProcessorAndMultipleFoldersAndFiles_whenCreateFile_thenHandleFile()" classname="report.processor.ReportProcessorIntegrationTests" time="0.013"/>
                  <testcase name="givenProcessorAndMultipleHandlers_whenCreateFile_thenHandleFile()" classname="report.processor.ReportProcessorIntegrationTests" time="0.017"/>
                  <testcase name="givenProcessor_whenShutdownGracefully_thenHandleAllExistingFiles()" classname="report.processor.ReportProcessorIntegrationTests" time="0.038"/>
                  <system-out><![CDATA[]]></system-out>
                  <system-err><![CDATA[]]></system-err>
                </testsuite>
                """;
    }

    private ReportProcessor reportProcessor() {
        return ReportProcessors.newXmlReportProcessor(10);
    }

    private class ExceptionThrowingReportHandler implements ReportHandler {
        @Override
        public void handle(FileInfo fileInfo) {
            throw new RuntimeException("I don't like this report");
        }
    }

    private class XmlParsingReportHandler implements ReportHandler {
        private AtomicInteger testcaseCount = new AtomicInteger();
        private AtomicInteger parsedCount = new AtomicInteger();
        private AtomicInteger notParsedCount = new AtomicInteger();
        private AtomicInteger skippedCount = new AtomicInteger();

        public ConcurrentHashMap<Path, Boolean> uniqueFiles = new ConcurrentHashMap<>();

        public int getParsedCount() {
            return parsedCount.get();
        }

        public int getNotParsedCount() {
            return notParsedCount.get();
        }

        public int getTestcaseCount() {
            return testcaseCount.get();
        }

        public int getSkippedCount() {
            return skippedCount.get();
        }

        public int getUniqueFilesCount() {
            return uniqueFiles.size();
        }

        @Override
        public void handle(FileInfo fileInfo) {
            var filePath = fileInfo.filePath();
            if (uniqueFiles.containsKey(filePath)) {
                skippedCount.incrementAndGet();
            } else {
                var factory = DocumentBuilderFactory.newInstance();
                try {
                    var builder = factory.newDocumentBuilder();
                    var document = builder.parse(filePath.toFile());
                    var testcasesCountDelta = document.getElementsByTagName("testcase").getLength();
                    if (null == uniqueFiles.putIfAbsent(fileInfo.filePath(), true)) {
                        testcaseCount.addAndGet(testcasesCountDelta);
                    }
                    parsedCount.incrementAndGet();
                } catch (Exception e) {
                    notParsedCount.incrementAndGet();
                }
            }
        }
    }

    @Test
    public void endToEndTest() throws InterruptedException, ExecutionException {
        var executorService = Executors.newCachedThreadPool();

        // given
        var folderPath1 = folderPath(testFolder);
        var folderPath2 = folderPath(testFolder);
        var reportType1 = "reportType1";
        var reportType2 = "reportType2";
        var reportType3 = "reportType3";
        var fileContent1 = fileContent1();

        var reportProcessor = reportProcessor();

        var exceptionThrowingReportHandler = new ExceptionThrowingReportHandler();
        var xmlParsingReportHandler1 = new XmlParsingReportHandler();
        var xmlParsingReportHandler2 = new XmlParsingReportHandler();
        var xmlParsingReportHandler3 = new XmlParsingReportHandler();

        // add folder
        reportProcessor.addMonitoredFolder(folderPath1, reportType1);
        reportProcessor.addMonitoredFolder(folderPath2, reportType2, reportType3);
        // register handler
        reportProcessor.registerHandler(exceptionThrowingReportHandler, reportType1);
        reportProcessor.registerHandler(xmlParsingReportHandler1, reportType1);
        reportProcessor.registerHandler(xmlParsingReportHandler2, reportType2);
        reportProcessor.registerHandler(xmlParsingReportHandler3, reportType3);

        var handlers = new ArrayList<XmlParsingReportHandler>();
        for (var i = 0; i < 10; i++) {
            var handler = new XmlParsingReportHandler();
            handlers.add(handler);
            reportProcessor.registerHandler(handler, reportType1, reportType2, reportType3);
        }

        // when
        // start
        reportProcessor.start();
        // create folders
        createFolder(folderPath1);
        createFolder(folderPath2);

        var fileCreationFuture = executorService.submit(() -> {
            // create files
            for (int i = 0; i < 25; i++) {
                createFile(filePath(folderPath1), fileContent1);
                createFile(filePath(folderPath2), fileContent1);
            }
        });

        // wait for file creation to finish
        fileCreationFuture.get();
        // stop
        reportProcessor.shutdown();

        // then
        // verify stopped
        assertTrue(reportProcessor.awaitTerminationMillis(terminationTimeoutMillis));

        // verify parsed data
        assertEquals(25, xmlParsingReportHandler1.getUniqueFilesCount());
        assertEquals(125, xmlParsingReportHandler1.getTestcaseCount());

        assertEquals(25, xmlParsingReportHandler2.getUniqueFilesCount());
        assertEquals(125, xmlParsingReportHandler2.getTestcaseCount());

        assertEquals(25, xmlParsingReportHandler3.getUniqueFilesCount());
        assertEquals(125, xmlParsingReportHandler3.getTestcaseCount());

        for (var handler : handlers) {
            assertEquals(50, handler.getUniqueFilesCount());
            assertEquals(250, handler.getTestcaseCount());
        }
    }
}
