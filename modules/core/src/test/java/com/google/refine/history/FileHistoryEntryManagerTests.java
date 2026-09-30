
package com.google.refine.history;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.io.FileHistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.TestUtils;

public class FileHistoryEntryManagerTests extends RefineTest {

    Project project;
    FileHistoryEntryManager sut = new FileHistoryEntryManager();

    @BeforeMethod
    public void setUp() {
        project = mock(Project.class);
        OperationRegistry.registerOperation(getCoreModule(), "mock-operation", HistoryEntryTests.MockOperation.class);
    }

    @Test
    public void testWriteHistoryEntry() throws IOException {
        StringWriter writer = new StringWriter();
        HistoryEntry historyEntry = HistoryEntry.load(project, HistoryEntryTests.fullJson);
        Properties options = new Properties();
        options.setProperty("mode", "save");
        sut.save(historyEntry, writer, options);
        TestUtils.equalAsJson(HistoryEntryTests.fullJson, writer.toString());
    }

    @Test
    public void testFindMissingChangeClasses() throws IOException {
        File projectDir = TestUtils.createTempDirectory("FileHistoryEntryManagerTest");
        File historyDir = new File(projectDir, FileHistoryEntryManager.HISTORY_DIR);
        historyDir.mkdirs();

        writeChangeFile(new File(historyDir, "1.change.zip"), Change.class.getName());
        writeChangeFile(new File(historyDir, "2.change.zip"), "org.example.extension.MissingChange");

        List<String> missingClasses = FileHistoryEntryManager.findMissingChangeClasses(projectDir);
        Assert.assertEquals(missingClasses, List.of("org.example.extension.MissingChange"));
    }

    @Test
    public void testFindMissingChangeClassesNoHistory() throws IOException {
        File projectDir = TestUtils.createTempDirectory("FileHistoryEntryManagerTest");

        Assert.assertTrue(FileHistoryEntryManager.findMissingChangeClasses(projectDir).isEmpty());
    }

    @Test
    public void testFindMissingChangeClassesInsideMassChange() throws IOException {
        // A MassChange (as produced by "Apply operations" or similar batch operations) bundling a core change
        // with an extension's change, in the exact format MassChange#save / History#writeOneChange produce.
        File projectDir = TestUtils.createTempDirectory("FileHistoryEntryManagerTest");
        File historyDir = new File(projectDir, FileHistoryEntryManager.HISTORY_DIR);
        historyDir.mkdirs();

        writeChangeFileContent(new File(historyDir, "1.change.zip"),
                "1\n" +
                        "com.google.refine.model.changes.MassChange\n" +
                        "updateRowContextDependencies=false\n" +
                        "changeCount=2\n" +
                        "1\n" +
                        "com.google.refine.model.changes.RowFlagChange\n" +
                        "row=0\n" +
                        "newFlagged=true\n" +
                        "oldFlagged=false\n" +
                        "/ec/\n" +
                        "1\n" +
                        "org.example.extension.MissingChange\n" +
                        "/ec/\n");

        List<String> missingClasses = FileHistoryEntryManager.findMissingChangeClasses(projectDir);
        Assert.assertEquals(missingClasses, List.of("org.example.extension.MissingChange"));
    }

    @Test
    public void testFindMissingChangeClassesStopsAfterFirstUnresolvable() throws IOException {
        // Once a nested sub-change's class can't be resolved, we don't know where its content ends, so we can't
        // locate any sibling sub-changes recorded after it -- even if they're also missing.
        File projectDir = TestUtils.createTempDirectory("FileHistoryEntryManagerTest");
        File historyDir = new File(projectDir, FileHistoryEntryManager.HISTORY_DIR);
        historyDir.mkdirs();

        writeChangeFileContent(new File(historyDir, "1.change.zip"),
                "1\n" +
                        "com.google.refine.model.changes.MassChange\n" +
                        "updateRowContextDependencies=false\n" +
                        "changeCount=2\n" +
                        "1\n" +
                        "org.example.extension.MissingChange\n" +
                        "1\n" +
                        "org.example.extension.AnotherMissingChange\n" +
                        "/ec/\n");

        List<String> missingClasses = FileHistoryEntryManager.findMissingChangeClasses(projectDir);
        Assert.assertEquals(missingClasses, List.of("org.example.extension.MissingChange"));
    }

    private void writeChangeFile(File file, String className) throws IOException {
        writeChangeFileContent(file, "1\n" + className + "\n");
    }

    private void writeChangeFileContent(File file, String content) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            out.putNextEntry(new ZipEntry("change.txt"));
            out.write(content.getBytes("UTF-8"));
            out.closeEntry();
        }
    }
}
