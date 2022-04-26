package report.processor;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * An immutable file information object. Contains information about file path, creation time and modification time.
 */
public record FileInfo(Path filePath, FileTime creationTime, FileTime modificationTime) {
}
