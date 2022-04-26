package report.processor.impl;

import report.processor.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * A {@link FileSystemHelper} that uses nio API internally.
 */
public class NioFileSystemHelper implements FileSystemHelper {
    private static final Logger logger = Logger.getLogger(NioFileSystemHelper.class.getName());

    public Collection<FileInfo> getFileInfos(Path folder) {
        var fileInfos = new ArrayList<FileInfo>();
        try {
            if (Files.exists(folder)) {
                try (Stream<Path> files = Files.list(folder)) {
                    files
                            .filter(filePath -> !Files.isDirectory(filePath))
                            .forEach(filePath -> {
                                try {
                                    var attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                                    var fileInfo = new FileInfo(filePath, attr.creationTime(), attr.lastModifiedTime());
                                    fileInfos.add(fileInfo);
                                } catch (IOException e) {
                                    // can't get last modified time for this file, but let's proceed with the other files
                                    logger.warning(e.getMessage());
                                }
                            });
                }
            }
        } catch (IOException e) {
            // can't list files in this folder
            logger.warning(e.getMessage());
        }
        return fileInfos;
    }
}
