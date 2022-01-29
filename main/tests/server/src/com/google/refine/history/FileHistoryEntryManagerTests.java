
package com.google.refine.history;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.io.FileHistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnAdditionOperation;
import com.google.refine.util.TestUtils;

public class FileHistoryEntryManagerTests extends RefineTest {

    Project project;
    FileHistoryEntryManager sut = new FileHistoryEntryManager();

    @BeforeMethod
    public void setUp() {
        project = mock(Project.class);
        OperationRegistry.registerOperation(getCoreModule(), "column-addition", ColumnAdditionOperation.class);
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
}
