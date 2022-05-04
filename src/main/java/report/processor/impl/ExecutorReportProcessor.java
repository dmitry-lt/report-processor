package report.processor.impl;

import report.processor.ReportHandler;
import report.processor.ReportProcessor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A {@link ReportProcessor} that periodically scans all the monitored folders with a configured delay
 * and calls the respective handlers asynchronously.
 */
public class ExecutorReportProcessor implements ReportProcessor {
    private static final Logger logger = Logger.getLogger(ExecutorReportProcessor.class.getName());
    private final long fileMonitoringPeriodMillis;

    private final ConcurrentHashMap<Path, MonitoredFolder> monitoredFolders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ReportHandler, ReportHandlerWrapper> handlers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService fileWatcherScheduler = defaultFileWatcherScheduler();
    private final ExecutorService fileHandlerService = defaultFileHandlerService();
    private final FileSystemHelper fileSystemHelper;
    private final Predicate<Path> filePathFilter;

    private volatile State state = State.NOT_STARTED;

    private static ScheduledExecutorService defaultFileWatcherScheduler() {
        return Executors.newScheduledThreadPool(1);
    }

    private static ExecutorService defaultFileHandlerService() {
        return new ForkJoinPool(Runtime.getRuntime().availableProcessors() - 1);
    }

    /**
     * Create a new {@code ExecutorReportProcessor}.
     *
     * @param fileSystemHelper           a helper object to get file information from the file system
     * @param filePathFilter             a filter ho monitor only changes in certain files, for example, XML files
     * @param fileMonitoringPeriodMillis delay in milliseconds to scan for file changes
     */
    public ExecutorReportProcessor(FileSystemHelper fileSystemHelper, Predicate<Path> filePathFilter, long fileMonitoringPeriodMillis) {
        this.fileSystemHelper = fileSystemHelper;
        this.filePathFilter = filePathFilter;
        this.fileMonitoringPeriodMillis = fileMonitoringPeriodMillis;
    }

    @Override
    public boolean addMonitoredFolder(Path folderPath, String... reportTypes) {
        if (null == monitoredFolders.putIfAbsent(folderPath, new MonitoredFolder(folderPath, filePathFilter, reportTypes, now()))) {
            logger.fine("monitored folder added: %s %s".formatted(folderPath, String.join(", ", reportTypes)));
            return true;
        } else {
            logger.fine("monitored folder not added: %s %s".formatted(folderPath, String.join(", ", reportTypes)));
            return false;
        }
    }

    @Override
    public boolean removeMonitoredFolder(Path folderPath) {
        var monitoringInfo = monitoredFolders.remove(folderPath);
        if (null != monitoringInfo) {
            var stopTime = now();
            monitoringInfo.limitMonitoringStopTime(stopTime);
            // finish file processing in the removed folder
            fileWatcherScheduler.submit(() -> submitFileChanges(monitoringInfo));
            logger.fine("monitored folder removed: %s %s".formatted(stopTime, folderPath));
            return true;
        } else {
            logger.fine("monitored folder not removed: %s".formatted(folderPath));
            return false;
        }
    }

    @Override
    public boolean registerHandler(ReportHandler handler, String... reportTypes) {
        if (null == handlers.putIfAbsent(handler, new ReportHandlerWrapper(handler, reportTypes))) {
            logger.fine("handler registered: %s %s".formatted(handler, String.join(", ", reportTypes)));
            return true;
        } else {
            logger.fine("handler not registered: %s".formatted(handler));
            return false;
        }
    }

    @Override
    public boolean unregisterHandler(ReportHandler handler) {
        var handlerWrapper = handlers.remove(handler);
        if (null != handlerWrapper) {
            handlerWrapper.markUnregistered();
            logger.fine("handler unregistered: %s".formatted(handler));
            return true;
        } else {
            logger.fine("handler not unregistered: %s".formatted(handler));
            return false;
        }
    }

    @Override
    public synchronized void start() {
        if (state == State.NOT_STARTED) {
            state = State.RUNNING;
            var startTime = now();
            monitoredFolders.forEachValue(1, monitoredFolder -> monitoredFolder.limitMonitoringStartTime(startTime));
            fileWatcherScheduler.scheduleWithFixedDelay(() -> submitFileChanges(monitoredFolders.values()), 0, fileMonitoringPeriodMillis, MILLISECONDS);
            logger.fine("processing started at %s".formatted(startTime));
        }
    }

    @Override
    public synchronized void shutdown() {
        if (state == State.RUNNING) {
            state = State.STOPPING_GRACEFULLY;
            var stopTime = now();
            monitoredFolders.values().forEach(monitoredFolder -> monitoredFolder.limitMonitoringStopTime(stopTime));
            // finish processing all the existing files
            fileWatcherScheduler.submit(() -> {
                submitFileChanges(monitoredFolders.values());
                fileWatcherScheduler.shutdown();
                fileHandlerService.shutdown();
            });
            logger.fine("processing graceful shutdown started at %s".formatted(stopTime));
        }
    }

    @Override
    public void shutdownNow() {
        state = State.STOPPING;
        fileWatcherScheduler.shutdownNow();
        fileHandlerService.shutdownNow();
        logger.fine("processing forced shutdown started");
    }

    @Override
    public boolean awaitTerminationMillis(long timeoutMillis) throws InterruptedException {
        var terminated = false;
        var start = System.currentTimeMillis();
        if (fileWatcherScheduler.awaitTermination(timeoutMillis, MILLISECONDS)) {
            var diff = System.currentTimeMillis() - start;
            terminated = fileHandlerService.awaitTermination(Math.max(0, timeoutMillis - diff), MILLISECONDS);
        }
        return terminated;
    }

    private Collection<ReportHandlerWrapper> handlersForReportTypes(Collection<String> reportTypes) {
        return handlers.values().stream().filter(hw -> reportTypes.stream().anyMatch(hw::handlesReportType)).toList();
    }

    private void submitFileChanges(MonitoredFolder monitoredFolder) {
        var fileInfos = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        var reportTypes = monitoredFolder.getReportTypes();
        var handlers = handlersForReportTypes(reportTypes);
        for (var fileInfo : fileInfos) {
            for (var handler : handlers) {
                fileHandlerService.submit(() -> handler.handle(fileInfo));
            }
        }
    }

    private void submitFileChanges(Collection<MonitoredFolder> monitoredFolders) {
        for (var monitoredFolder : monitoredFolders) {
            submitFileChanges(monitoredFolder);
        }
    }

    private enum State {NOT_STARTED, RUNNING, STOPPING_GRACEFULLY, STOPPING}

}
