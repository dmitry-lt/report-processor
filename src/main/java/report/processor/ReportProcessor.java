package report.processor;

import java.nio.file.Path;

/**
 * Monitors folders for file changes and registers handlers to handle the changes.
 * A folder is associated with one or many report types. A handler handles one or many report types.
 * Every file change event in a folder is propagated to all the handlers that handle any of the report types associated
 * with the folder.
 */
public interface ReportProcessor {
    /**
     * Add a new folder to monitoring. The same folder can't be added twice.
     *
     * @param folderPath  folder path
     * @param reportTypes associated report types
     * @return {@code true} if the folder was added, {@code false} otherwise
     */
    boolean addMonitoredFolder(Path folderPath, String... reportTypes);

    /**
     * Remove a folder from monitoring. All the existing files in the folder will be handled.
     *
     * @param folderPath folder path
     * @return {@code true} if the folder was removed, {@code false} otherwise
     */
    boolean removeMonitoredFolder(Path folderPath);

    /**
     * Register a new handler. The same handler  can't be registered twice.
     *
     * @param handler     handler
     * @param reportTypes associated report types
     * @return {@code true} if the handler was registered, {@code false} otherwise
     */
    boolean registerHandler(ReportHandler handler, String... reportTypes);

    /**
     * Unregister a handler.
     *
     * @param handler handler
     * @return {@code true} if the handler was unregistered, {@code false} otherwise
     */
    boolean unregisterHandler(ReportHandler handler);

    /**
     * Start monitoring.
     */
    void start();

    /**
     * Shutdown monitoring gracefully. All the existing files in the monitored folders will be handled.
     */
    void shutdown();

    /**
     * Force shutdown monitoring. Some existing files in the monitored folders might not be handled.
     */
    void shutdownNow();

    /**
     * Blocks until all the files have been processed after a shutdown request, or the timeout occurs,
     * or the current thread is interrupted, whichever happens first.
     *
     * @param timeoutMillis the maximum time to wait in milliseconds
     * @return {@code true} if all the files have been processed, {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTerminationMillis(long timeoutMillis) throws InterruptedException;
}
