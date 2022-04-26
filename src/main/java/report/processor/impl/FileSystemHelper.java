package report.processor.impl;

import report.processor.FileInfo;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Gets file information from the file system
 */
interface FileSystemHelper {
    /**
     * Get file information for all the files in a folder.
     *
     * @param folder folder
     * @return file information for all the files in the folder
     */
    Collection<FileInfo> getFileInfos(Path folder);
}
