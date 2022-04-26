package report.processor;

import report.processor.impl.ExecutorReportProcessor;
import report.processor.impl.NioFileSystemHelper;

/**
 * Factory methods for {@code ReportProcessor}.
 */
public class ReportProcessors {
    /**
     * Create a new report processor to handle XML files.
     *
     * @return report processor
     */
    public static ReportProcessor newXmlReportProcessor() {
        return newXmlReportProcessor(1000);
    }

    /**
     * Create a new report processor to handle XML files with specified delay in milliseconds to scan for file changes.
     *
     * @param fileMonitoringPeriodMillis delay in milliseconds to scan for file changes
     * @return report processor
     */
    public static ReportProcessor newXmlReportProcessor(long fileMonitoringPeriodMillis) {
        return new ExecutorReportProcessor(
                new NioFileSystemHelper(),
                filePath -> filePath.toString().toLowerCase().endsWith(".xml"),
                fileMonitoringPeriodMillis);
    }
}
