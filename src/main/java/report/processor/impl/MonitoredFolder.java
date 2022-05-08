package report.processor.impl;

import report.processor.FileInfo;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A monitored folder monitors files in a specific folder during a configured time window.
 */
class MonitoredFolder {
    private static final Logger logger = Logger.getLogger(MonitoredFolder.class.getName());

    private final Path folderPath;
    private final Predicate<Path> filePathFilter;
    private final ConcurrentHashMap<Path, FileInfo> fileInfosMap = new ConcurrentHashMap<>();
    private final Set<String> reportTypes;

    private volatile FileTime monitoringStartTime;

    /**
     * Create a new monitored folder.
     *
     * @param folderPath     folder path to monitor
     * @param filePathFilter filter to monitor only certain files, for example, XML
     * @param reportTypes    report types associated with the folder
     * @param startTime      monitoring start time
     */
    public MonitoredFolder(Path folderPath, Predicate<Path> filePathFilter, String[] reportTypes, Instant startTime) {
        this.folderPath = folderPath;
        this.filePathFilter = filePathFilter;
        this.reportTypes = Arrays.stream(reportTypes).collect(Collectors.toSet());
        this.monitoringStartTime = FileTime.from(startTime);
    }

    /**
     * Get report types associated with the folder.
     *
     * @return report types
     */
    public Collection<String> getReportTypes() {
        return reportTypes;
    }

    /**
     * Sets the new monitoring start time if it is later than the old one
     *
     * @param startTime new monitoring start time
     */
    public void limitMonitoringStartTime(Instant startTime) {
        var newTime = FileTime.from(startTime);
        if (this.monitoringStartTime.compareTo(newTime) < 0) {
            this.monitoringStartTime = newTime;
        }
    }

    private boolean isTimeInMonitoredWindow(FileTime time) {
        return time.compareTo(monitoringStartTime) >= 0;
    }

    private boolean isInMonitoredWindow(FileInfo fileInfo) {
        logger.finest("checking if file creation or modification time is in monitored window: %s or %s >= %s"
                .formatted(fileInfo.creationTime(), fileInfo.modificationTime(), monitoringStartTime));
        return isTimeInMonitoredWindow(fileInfo.creationTime()) || isTimeInMonitoredWindow(fileInfo.modificationTime());
    }

    /**
     * Get all the file changes since the previous call of this method.
     *
     * @param fileSystemHelper a helper object to get file information from the file system
     * @return new monitored changes
     */
    public synchronized Collection<FileInfo> getNewMonitoredChanges(FileSystemHelper fileSystemHelper) {
        var monitoredChanges = new ArrayList<FileInfo>();
        var newFileInfos = fileSystemHelper.getFileInfos(folderPath, filePathFilter);
        var newFileInfosMap = new HashMap<Path, FileInfo>();
        for (var newFileInfo : newFileInfos) {
            if (filePathFilter.test(newFileInfo.filePath())) {
                newFileInfosMap.put(newFileInfo.filePath(), newFileInfo);
            }
        }
        for (var filePath : fileInfosMap.keySet()) {
            if (!newFileInfosMap.containsKey(filePath)) {
                // file deleted
                fileInfosMap.remove(filePath);
            }
        }
        for (var filePath : newFileInfosMap.keySet()) {
            var oldFileInfo = fileInfosMap.get(filePath);
            var newFileInfo = newFileInfosMap.get(filePath);
            if (!fileInfosMap.containsKey(filePath) || !newFileInfo.equals(oldFileInfo)) {
                // file created or updated
                fileInfosMap.put(filePath, newFileInfo);
                if (isInMonitoredWindow(newFileInfo)) {
                    monitoredChanges.add(newFileInfo);
                }
            }
        }
        return monitoredChanges;
    }
}
