
package org.openrefine.runners.spark;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.spark.api.java.JavaPairRDD;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.Tuple2;

import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.overlay.OverlayModelResolver;
import org.openrefine.util.TestUtils;

public class SparkGridTests extends SparkBasedTest {

    protected static class MyOverlayModel implements OverlayModel {

        @JsonProperty("foo")
        String myProperty = "bar";

        public boolean equals(Object other) {
            return other instanceof MyOverlayModel;
        }
    }

    protected SparkGrid state;
    protected List<Tuple2<Long, Row>> rows;

    @BeforeMethod
    public void createGrid() {
        // Create sample grid
        JavaPairRDD<Long, Row> grid = rowRDD(new Serializable[][] {
                { 1, 2, "3" },
                { 4, "5", true }
        });
        // and a column model
        ColumnModel cm = new ColumnModel(
                Arrays.asList(
                        new ColumnMetadata("a"),
                        new ColumnMetadata("b"),
                        new ColumnMetadata("c")));

        OverlayModelResolver.registerOverlayModel("mymodel", MyOverlayModel.class);
        state = new SparkGrid(cm, grid, Collections.singletonMap("mymodel", new MyOverlayModel()), new SparkRunner(context));

        rows = new ArrayList<>();
        rows.add(new Tuple2<Long, Row>(0L, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null), new Cell("3", null)))));
        rows.add(new Tuple2<Long, Row>(1L, new Row(Arrays.asList(new Cell(4, null), new Cell("5", null), new Cell(true, null)))));
    }

    @Test
    public void testDatamodelRunner() {
        Assert.assertTrue(state.getRunner() instanceof SparkRunner);
    }

    @Test
    public void testSize() {
        Assert.assertEquals(state.rowCount(), 2);
    }

    @Test
    public void testToString() {
        Assert.assertEquals(state.toString(), "[Grid, 3 columns, 2 rows]");
    }

    @Test
    public void testGetGrid() {
        JavaPairRDD<Long, IndexedRow> grid = state.getGrid();
        Row row1 = grid.lookup(0L).get(0).getRow();
        Assert.assertEquals(row1.getCellValue(0), 1);
        Assert.assertEquals(row1.getCellValue(1), 2);
        Assert.assertEquals(row1.getCellValue(2), "3");
        Row row2 = grid.lookup(1L).get(0).getRow();
        Assert.assertEquals(row2.getCellValue(0), 4);
        Assert.assertEquals(row2.getCellValue(1), "5");
        Assert.assertEquals(row2.getCellValue(2), true);
    }

    @Test
    public void testSaveAndLoad() throws IOException {
        File tempFile = TestUtils.createTempDirectory("testgrid");
        state.saveToFile(tempFile);

        SparkGrid loaded = SparkGrid.loadFromFile(context(), tempFile);

        Assert.assertEquals(loaded.getOverlayModels(), state.getOverlayModels());
        List<Tuple2<Long, IndexedRow>> loadedGrid = loaded.getGrid().collect();
        Assert.assertEquals(loadedGrid, state.getGrid().collect());
    }

    @Test
    public void testGetAllRecords() {
        JavaPairRDD<Long, org.openrefine.model.Record> records = state.getRecords();

        Assert.assertEquals(records.count(), 2L);
        // extracting records again returns the exact same RDD, as it is cached
        Assert.assertEquals(state.getRecords(), records);
    }

    @Test
    public void testGetRecords() {
        Assert.assertEquals(state.getRecordsAfter(1L, 10),
                Collections.singletonList(new org.openrefine.model.Record(1L, Collections.singletonList(rows.get(1)._2))));
    }

    @Test
    public void testGetRecord() {
        Record firstRecord = state.getRecord(0L);
        Assert.assertEquals(firstRecord.getStartRowId(), 0L);
        Assert.assertEquals(firstRecord.getRows().size(), 1);
        Assert.assertEquals(firstRecord.getRows().get(0), rows.get(0)._2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetNoRecord() {
        state.getRecord(3L);
    }
}
