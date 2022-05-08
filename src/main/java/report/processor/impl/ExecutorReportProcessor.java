package report.processor.impl;

import report.processor.ReportHandler;
import report.processor.ReportProcessor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static java.time.Instant.now;
import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A {@link ReportProcessor} that periodically scans all the monitored folders with a configured delay
 * and calls the respective handlers asynchronously.
 */
public class ExecutorReportProcessor implements ReportProcessor {
    private static final Logger logger = Logger.getLogger(ExecutorReportProcessor.class.getName());
    private final long fileMonitoringPeriodMillis;

    private final ConcurrentHashMap<Path, MonitoredFolder> monitoredFolders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<ReportHandler>> handlers = new ConcurrentHashMap<>();

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
        var monitoredFolder = monitoredFolders.remove(folderPath);
        if (null != monitoredFolder) {
            // finish file processing in the removed folder
            submitFileChanges(monitoredFolder);
            logger.fine("monitored folder removed: %s".formatted(folderPath));
            return true;
        } else {
            logger.fine("monitored folder not removed: %s".formatted(folderPath));
            return false;
        }
    }

    @Override
    public synchronized boolean registerHandler(ReportHandler handler, String... reportTypes) {
        logger.fine("registering handler: %s %s".formatted(handler, String.join(", ", reportTypes)));
        var handlerRegistered = true;
        for (var reportType : reportTypes) {
            if (!handlers.containsKey(reportType)) {
                handlers.put(reportType, synchronizedSet(new HashSet<>()));
            }
            handlerRegistered &= handlers.get(reportType).add(handler);
        }
        return handlerRegistered;
    }

    @Override
    public synchronized boolean unregisterHandler(ReportHandler handler) {
        logger.fine("unregistering handler: %s".formatted(handler));
        var handlerUnregistered = false;
        for (var handlerSet : handlers.values()) {
            handlerUnregistered |= handlerSet.remove(handler);
        }
        return handlerUnregistered;
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
            // finish processing all the existing files
            fileWatcherScheduler.shutdown();
            submitFileChanges(monitoredFolders.values());
            fileHandlerService.shutdown();
            logger.fine("processing graceful shutdown started");
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

    private void submitFileChanges(MonitoredFolder monitoredFolder) {
        var fileInfos = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        var reportTypes = monitoredFolder.getReportTypes();
        Set<ReportHandler> currentHandlers = new HashSet<>();
        for (var reportType : reportTypes) {
            currentHandlers.addAll(handlers.getOrDefault(reportType, emptySet()));
        }
        for (var fileInfo : fileInfos) {
            for (var handler : currentHandlers) {
                fileHandlerService.submit(() -> {
                    // check that the handler is still registered
                    if (handlers.values().stream().anyMatch(set -> set.contains(handler))) {
                        handler.handle(fileInfo);
                    }
                });
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
