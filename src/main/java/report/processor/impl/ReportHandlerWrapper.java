package report.processor.impl;

import report.processor.FileInfo;
import report.processor.ReportHandler;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A wrapper around a {@link ReportHandler} used to associate the handler with report types and indicate if the handler
 * is registered in its report processor.
 */
class ReportHandlerWrapper {
    private final ReportHandler handler;
    private final Set<String> reportTypes;
    private volatile boolean isRegistered = true;

    /**
     * Creates a new report handler wrapper
     *
     * @param handler     handler
     * @param reportTypes associated report types
     */
    public ReportHandlerWrapper(ReportHandler handler, String[] reportTypes) {
        this.handler = handler;
        this.reportTypes = Arrays.stream(reportTypes).collect(Collectors.toSet());
    }

    /**
     * Marks the handler as not registered anymore.
     */
    public void markUnregistered() {
        isRegistered = false;
    }

    /**
     * If the handler is registered in its report processor, handle the file change event.
     *
     * @param fileInfo file information
     */
    public void handle(FileInfo fileInfo) {
        if (isRegistered) {
            handler.handle(fileInfo);
        }
    }

    /**
     * Checks if a report type can be handled by this handler.
     *
     * @param reportType report type
     * @return {@code true} if the report type can be handled, {@code false} otherwise
     */
    public boolean handlesReportType(String reportType) {
        return reportTypes.contains(reportType);
    }
}
