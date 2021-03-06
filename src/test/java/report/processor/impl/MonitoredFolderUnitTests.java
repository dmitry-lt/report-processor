package report.processor.impl;

import org.junit.jupiter.api.Test;
import report.processor.FileInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import static java.time.Instant.EPOCH;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonitoredFolderUnitTests {
    static class FakeFileSystemHelper implements FileSystemHelper {
        public ArrayList<FileInfo> fileInfos = new ArrayList<>();

        @Override
        public Collection<FileInfo> getFileInfos(Path folder, Predicate<Path> filePathFilter) {
            return fileInfos;
        }
    }

    private FakeFileSystemHelper fileSystemHelper() {
        return new FakeFileSystemHelper();
    }

    private boolean isXml(Path filePath) {
        return filePath.toString().toLowerCase().endsWith(".xml");
    }

    private MonitoredFolder monitoredFolder() {
        return new MonitoredFolder(Paths.get("test_path"), this::isXml, new String[]{"report_type_1", "report_type_2"}, now());
    }

    @Test
    public void givenMonitoredFolder_whenCreateXmlFile_thenGetMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(now()), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);

        // then
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(monitoredChanges, changes);
    }

    @Test
    public void givenMonitoredFolder_whenModifyXmlFile_thenGetMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(EPOCH), FileTime.from(now())));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);

        // then
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(monitoredChanges, changes);
    }

    @Test
    public void givenMonitoredFolder_whenXmlFileChangeInThePast_thenDontGetMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(EPOCH), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);

        // then
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(0, changes.size());
    }

    @Test
    public void givenMonitoredFolder_whenDeleteXmlFile_thenDontGetMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(now()), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(monitoredChanges, changes);

        // delete XML file
        fileSystemHelper.fileInfos.clear();

        // then
        changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(0, changes.size());
    }

    @Test
    public void givenMonitoredFolder_whenCreateNonXmlFile_thenDontGetMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        monitoredChanges.add(new FileInfo(Paths.get("file.txt"), FileTime.from(now()), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);

        // then
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(0, changes.size());
    }

    @Test
    public void givenDuplicateReportTypes_whenCreateMonitoredFolder_thenRemoveDuplicates() {
        // given
        var reportTypes = new String[]{"report_type_1", "report_type_1"};

        // when
        // create monitored folder
        var monitoredFolder = new MonitoredFolder(Paths.get("test_path"), this::isXml, reportTypes, now());

        // then
        // assert reportTypes are as expected
        assertEquals(Set.of("report_type_1"), monitoredFolder.getReportTypes());
    }

    @Test
    public void givenMonitoredFolder_whenSubsequentlyGetMonitoredChanges_thenGetEmptyMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(now()), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        assertEquals(monitoredChanges, changes);
        changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);

        // then
        // assert changes are as expected
        assertEquals(0, changes.size());
    }

    @Test
    public void givenMonitoredFolder_whenSubsequentlyCreateXmlFile_thenGetMonitoredChanges() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        var creationTime = now();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(creationTime), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(monitoredChanges, changes);

        // delete and re-create file
        monitoredChanges.clear();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(creationTime.plusMillis(1)), FileTime.from(EPOCH)));
        fileSystemHelper.fileInfos.clear();
        fileSystemHelper.fileInfos.addAll(monitoredChanges);

        // then
        changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(monitoredChanges, changes);
    }

    @Test
    public void givenMonitoredFolder_whenIncreaseStartTime_thenDontHandlePastEvents() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        var creationTime = now();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(creationTime), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);
        // increase start time
        monitoredFolder.limitMonitoringStartTime(creationTime.plusMillis(1));

        // then
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(0, changes.size());
    }

    @Test
    public void givenMonitoredFolder_whenDecreaseStartTime_thenStillDontHandlePastEvents() {
        // given
        var fileSystemHelper = fileSystemHelper();
        var monitoredFolder = monitoredFolder();
        var monitoredChanges = new ArrayList<FileInfo>();
        var creationTime = now();
        monitoredChanges.add(new FileInfo(Paths.get("file.xml"), FileTime.from(creationTime.minusMillis(2)), FileTime.from(EPOCH)));

        // when
        // create XML file
        fileSystemHelper.fileInfos.addAll(monitoredChanges);
        // decrease start time
        monitoredFolder.limitMonitoringStartTime(creationTime.minusMillis(1));

        // then
        var changes = monitoredFolder.getNewMonitoredChanges(fileSystemHelper);
        // assert changes are as expected
        assertEquals(0, changes.size());
    }
}
