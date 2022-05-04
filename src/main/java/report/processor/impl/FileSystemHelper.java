package report.processor.impl;

import report.processor.FileInfo;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Gets file information from the file system
 */
interface FileSystemHelper {
    /**
     * Get file information for all the files in a folder.
     *
     * @param folder         folder
     * @param filePathFilter filter to get file information only for certain files, for example, XML
     * @return file information for all the files in the folder
     */
    Collection<FileInfo> getFileInfos(Path folder, Predicate<Path> filePathFilter);
}
