
package org.openrefine.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.RowMapper;
import org.openrefine.model.Runner;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.GridCache;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
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

    public static class MyOperation extends RowMapOperation {

        protected MyOperation() {
            super(EngineConfig.ALL_ROWS);
        }

        // Deletes the first column of the table
        @Override
        public List<ColumnInsertion> getColumnInsertions() {
            return Collections.emptyList();
        }

        @Override
        public Set<String> getColumnDeletions() {
            return Collections.singleton("a");
        }

        @Override
        public String getDescription() {
            return "remove column \"a\"";
        }
    };

    @BeforeMethod
    public void setUp() throws IOException, OperationException {
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
        when(secondState.withOverlayModels(Mockito.any())).thenReturn(secondState);
        when(grid.saveToFileAsync(Mockito.any())).thenReturn(saveFuture);
        Operation operation = new MyOperation();
        gridStore = mock(GridCache.class);
        when(gridStore.listCachedGridIds()).thenReturn(Collections.emptySet());
        history = new History(grid, mock(ChangeDataStore.class), gridStore, 34983L);
        history.addEntry(1234L, operation);
        sut = new HistoryEntryManager();
    }

    @Test
    public void testSaveAndLoadHistory() throws IOException, OperationException {
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
