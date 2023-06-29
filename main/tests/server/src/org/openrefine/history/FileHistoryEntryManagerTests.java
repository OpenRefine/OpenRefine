
package org.openrefine.history;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.io.FileHistoryEntryManager;
import org.openrefine.model.Project;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.column.ColumnAdditionOperation;
import org.openrefine.util.TestUtils;

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
