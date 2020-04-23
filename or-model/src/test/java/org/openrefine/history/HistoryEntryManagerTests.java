
package org.openrefine.history;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.SparkBasedTest;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.operations.Operation.NotImmediateOperationException;
import org.openrefine.operations.UnknownOperation;
import org.openrefine.util.TestUtils;

public class HistoryEntryManagerTests extends SparkBasedTest {

    HistoryEntryManager sut;
    History history;

    public static class MyChange implements Change {

        // Deletes the first column of the table
        @Override
        public GridState apply(GridState projectState) {
            List<ColumnMetadata> columns = projectState.getColumnModel().getColumns();
            int rowSize = columns.size();
            JavaPairRDD<Long, Row> newGrid = projectState.getGrid().mapValues(r -> new Row(r.getCells().subList(1, rowSize)));
            List<ColumnMetadata> newColumns = columns.subList(1, columns.size());
            return new GridState(new ColumnModel(newColumns), newGrid, projectState.getOverlayModels());
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

    };

    @BeforeMethod
    public void setUp() throws NotImmediateOperationException {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("a"),
                new ColumnMetadata("b"),
                new ColumnMetadata("c")));
        JavaPairRDD<Long, Row> rows = rowRDD(new Cell[][] {
                { new Cell(1, null), new Cell(2, null), new Cell("3", null) },
                { new Cell(true, null), new Cell("b", null), new Cell(5, null) }
        });
        GridState gridState = new GridState(columnModel, rows, Collections.emptyMap());
        Change change = new MyChange();
        HistoryEntry entry = new HistoryEntry(1234L, "some description",
                new UnknownOperation("my-op", "some desc"), change);
        history = new History(gridState);
        history.addEntry(entry);
        sut = new HistoryEntryManager(context());
    }

    @Test
    public void testSaveAndLoadHistory() throws IOException {
        File tempFile = TestUtils.createTempDirectory("testhistory");
        sut.save(history, tempFile);

        History recovered = sut.load(tempFile);
        Assert.assertEquals(recovered.getPosition(), 1);
        GridState state = recovered.getCurrentGridState();
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 2);
    }

}
