package report.processor.impl;

import org.junit.jupiter.api.Test;
import report.processor.ReportHandler;

import java.nio.file.Paths;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ExecutorReportProcessorUnitTests {
    private ExecutorReportProcessor executorReportProcessor() {
        return new ExecutorReportProcessor(mock(FileSystemHelper.class), mock(Predicate.class), 100);
    }

    @Test
    public void givenProcessor_whenAddMonitoredFolder_thenCantAddSameFolderTwice() {
        // given
        var folder = Paths.get("1");
        var processor = executorReportProcessor();

        // when
        assertTrue(processor.addMonitoredFolder(folder, "1"));

        // then
        assertFalse(processor.addMonitoredFolder(folder, "1"));
    }

    @Test
    public void givenProcessor_whenRemoveMonitoredFolder_thenCantRemoveSameFolderTwice() {
        // given
        var folder = Paths.get("1");
        var processor = executorReportProcessor();

        // when
        assertTrue(processor.addMonitoredFolder(folder, "1"));
        assertTrue(processor.removeMonitoredFolder(folder));

        // then
        assertFalse(processor.removeMonitoredFolder(folder));
    }

    @Test
    public void givenProcessor_whenAddMonitoredFolder_thenCantRemoveNotAddedFolder() {
        // given
        var folder = Paths.get("1");
        var processor = executorReportProcessor();

        // when
        assertTrue(processor.addMonitoredFolder(folder, "1"));

        // then
        assertFalse(processor.removeMonitoredFolder(Paths.get("2")));
    }

    @Test
    public void givenProcessor_whenRegisterHandler_thenCantRegisterSameHandlerTwice() {
        // given
        var handler = mock(ReportHandler.class);
        var processor = executorReportProcessor();

        // when
        assertTrue(processor.registerHandler(handler, "1"));

        // then
        assertFalse(processor.registerHandler(handler, "1"));
    }

    @Test
    public void givenProcessor_whenUnregisterHandler_thenCantUnregisterSameHandlerTwice() {
        // given
        var handler = mock(ReportHandler.class);
        var processor = executorReportProcessor();

        // when
        assertTrue(processor.registerHandler(handler, "1"));
        assertTrue(processor.unregisterHandler(handler));

        // then
        assertFalse(processor.unregisterHandler(handler));
    }

    @Test
    public void givenProcessor_whenRegisterHandler_thenCantUnregisterNotRegisteredHandler() {
        // given
        var handler = mock(ReportHandler.class);
        var processor = executorReportProcessor();

        // when
        assertTrue(processor.registerHandler(handler, "1"));

        // then
        assertFalse(processor.unregisterHandler(mock(ReportHandler.class)));
    }

}
