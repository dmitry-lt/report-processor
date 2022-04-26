package report.processor;

/**
 * A handler to handle file change events. Since it is intended to be use in a multi-threaded environment,
 * it must be thread-safe. It also must handle repeating events for the same file.
 */
@FunctionalInterface
public interface ReportHandler {
    /**
     * Handle a file.
     *
     * @param fileInfo file information
     */
    void handle(FileInfo fileInfo);
}
