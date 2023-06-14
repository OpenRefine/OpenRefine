
package org.openrefine.commands.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.history.GridPreservation;
import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class GetOperationsCommandTests extends CommandTestBase {

    Project project;
    ProjectMetadata projectMetadata;
    History history;
    Grid grid;

    @BeforeMethod
    public void setUpProject() {
        project = mock(Project.class);
        when(project.getId()).thenReturn(1234L);
        history = mock(History.class);
        grid = mock(Grid.class);
        when(grid.rowCount()).thenReturn(5L);
        when(project.getCurrentGrid()).thenReturn(grid);
        projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getTags()).thenReturn(new String[] {});
        Instant now = Instant.now();
        when(projectMetadata.getModified()).thenReturn(now);
        when(project.getLastSave()).thenReturn(now);
        when(project.getHistory()).thenReturn(history);

        List<HistoryEntry> historyEntries = new ArrayList<>();
        Operation reproducibleOperation = new ReproducibleOp();
        historyEntries.add(new HistoryEntry(3487L, reproducibleOperation, GridPreservation.PRESERVES_RECORDS));
        Operation unreproducibleOperation = new UnreproducibleOp();
        historyEntries.add(new HistoryEntry(4589L, unreproducibleOperation, GridPreservation.PRESERVES_ROWS));

        when(history.getLastPastEntries(-1)).thenReturn(historyEntries);

        ProjectManager.singleton.registerProject(project, projectMetadata);

        command = new GetOperationsCommand();
    }

    @Test
    public void testCommand() throws Exception {
        when(request.getParameter("project")).thenReturn("1234");

        command.doGet(request, response);

        String expectedJson = "{\n"
                + "  \"entries\" : [ {\n"
                + "    \"description\" : \"some reproducible operation\",\n"
                + "    \"operation\" : {\n"
                + "      \"description\" : \"some reproducible operation\",\n"
                + "      \"op\" : \"core/reproducible-op\"\n"
                + "    }\n"
                + "  }, {\n"
                + "    \"description\" : \"unreproducible op\"\n"
                + "  } ]\n"
                + "}";

        verify(response).setStatus(200);
        JsonNode json = ParsingUtilities.mapper.readTree(writer.toString());
        TestUtils.isSerializedTo(json, expectedJson, ParsingUtilities.defaultWriter);
    }

    protected static class ReproducibleOp implements Operation {

        @Override
        public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDescription() {
            return "some reproducible operation";
        }

        @Override
        public String getOperationId() {
            return "core/reproducible-op";
        }
    }

    protected static class UnreproducibleOp implements Operation {

        @Override
        public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDescription() {
            return "unreproducible op";
        }

        @Override
        public String getOperationId() {
            return "core/unreproducible-op";
        }

        @Override
        public boolean isReproducible() {
            return false;
        }
    }
}
