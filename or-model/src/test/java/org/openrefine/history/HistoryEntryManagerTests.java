package org.openrefine.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.operations.Operation.NotImmediateOperationException;
import org.openrefine.operations.UnknownOperation;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HistoryEntryManagerTests {

	HistoryEntryManager sut;
	History history;
	DatamodelRunner runner;
	
	static RowMapper mapper = mock(RowMapper.class);
	
	public static class MyChange implements Change  {
		// Deletes the first column of the table
		@Override
		public GridState apply(GridState projectState, ChangeContext context) {
		    List<ColumnMetadata> columns = projectState.getColumnModel().getColumns();
            List<ColumnMetadata> newColumns = columns.subList(1, columns.size());
		    
            return projectState.mapRows(mapper, new ColumnModel(newColumns));
		}

        @Override
        public boolean isImmediate() {
            return false;
        }

        @Override
        public DagSlice getDagSlice() {
            return null;
        }
	};

    @BeforeMethod
    public void setUp() throws NotImmediateOperationException, IOException, DoesNotApplyException {
        runner = mock(DatamodelRunner.class);
    	ColumnModel columnModel = new ColumnModel(Arrays.asList(
    			new ColumnMetadata("a"),
    			new ColumnMetadata("b"),
    			new ColumnMetadata("c")));
    	GridState gridState = mock(GridState.class);
    	when(gridState.getColumnModel()).thenReturn(columnModel);
    	when(runner.loadGridState(Mockito.any())).thenReturn(gridState);
    	GridState secondState = mock(GridState.class);
    	when(secondState.getColumnModel()).thenReturn(new ColumnModel(columnModel.getColumns().subList(1, 3)));
    	when(gridState.mapRows((RowMapper)Mockito.any(), Mockito.any())).thenReturn(secondState);
    	Change change = new MyChange();
    	HistoryEntry entry = new HistoryEntry(1234L, "some description",
    			new UnknownOperation("my-op", "some desc"), change);
        history = new History(gridState, mock(ChangeDataStore.class));
        history.addEntry(entry);
        sut = new HistoryEntryManager(runner);
    }
    
	@Test
	public void testSaveAndLoadHistory() throws IOException, DoesNotApplyException {
		File tempFile = TestUtils.createTempDirectory("testhistory");
		sut.save(history, tempFile);
		
		History recovered = sut.load(tempFile);
		Assert.assertEquals(recovered.getPosition(), 1);
		GridState state = recovered.getCurrentGridState();
		Assert.assertEquals(state.getColumnModel().getColumns().size(), 2);
	}
	
	
}
