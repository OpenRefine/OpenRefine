
package org.openrefine.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.RowMapper;
import org.openrefine.model.Runner;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.GridCache;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.process.ProgressReporter;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.TestUtils;

public class HistoryEntryManagerTests {

    HistoryEntryManager sut;
    History history;
    Runner runner;
    GridCache gridStore;
    ProgressingFuture<Void> saveFuture;

    static RowMapper mapper = mock(RowMapper.class);

    public static class MyOperation implements Operation {

        // Deletes the first column of the table
        @Override
        public Change.ChangeResult apply(Grid projectState, ChangeContext context) {
            List<ColumnMetadata> columns = projectState.getColumnModel().getColumns();
            List<ColumnMetadata> newColumns = columns.subList(1, columns.size());

            return new Change.ChangeResult(
                    projectState.mapRows(mapper, new ColumnModel(newColumns)),
                    GridPreservation.PRESERVES_ROWS);
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

        @Override
        public Change createChange() throws ParsingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDescription() {
            return "remove the first column";
        }
    };

    @BeforeMethod
    public void setUp() throws IOException, DoesNotApplyException {
        OperationRegistry.registerOperation("core", "my-operation", MyOperation.class);
        runner = mock(Runner.class);
        saveFuture = mock(VoidFuture.class);
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("a"),
                new ColumnMetadata("b"),
                new ColumnMetadata("c")));
        Grid grid = mock(Grid.class);
        when(grid.getColumnModel()).thenReturn(columnModel);
        when(runner.loadGrid(Mockito.any())).thenReturn(grid);
        Grid secondState = mock(Grid.class);
        when(secondState.getColumnModel()).thenReturn(new ColumnModel(columnModel.getColumns().subList(1, 3)));
        when(grid.mapRows((RowMapper) Mockito.any(), Mockito.any())).thenReturn(secondState);
        when(grid.saveToFileAsync(Mockito.any())).thenReturn(saveFuture);
        Operation operation = new MyOperation();
        gridStore = mock(GridCache.class);
        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());
        history = new History(grid, mock(ChangeDataStore.class), gridStore, 34983L);
        history.addEntry(1234L, operation);
        sut = new HistoryEntryManager();
    }

    @Test
    public void testSaveAndLoadHistory() throws IOException, DoesNotApplyException {
        File tempFile = TestUtils.createTempDirectory("testhistory");
        sut.save(history, tempFile, mock(ProgressReporter.class));

        History recovered = sut.load(runner, tempFile, 34983L);
        Assert.assertEquals(recovered.getPosition(), 1);
        Grid state = recovered.getCurrentGrid();
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 2);
    }

    // for mocking purposes
    protected interface VoidFuture extends ProgressingFuture<Void> {

    }
}
