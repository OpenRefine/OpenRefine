
package org.openrefine.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.apache.spark.api.java.JavaPairRDD;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ColumnTests {

    ColumnMetadata metadata;
    JavaPairRDD<Long, Cell> cells;
    Cell cell;
    Column SUT;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setUpMocks() {
        metadata = mock(ColumnMetadata.class);
        cells = (JavaPairRDD<Long, Cell>) mock(JavaPairRDD.class);
        cell = mock(Cell.class);
        SUT = new Column(metadata, cells);
    }

    @Test
    public void testGetMetadata() {
        Assert.assertEquals(SUT.getMetadata(), metadata);
    }

    @Test
    public void testGetCells() {
        Assert.assertEquals(SUT.getCells(), cells);
    }

    @Test
    public void testCount() {
        when(cells.count()).thenReturn(345L);
        Assert.assertEquals(SUT.size(), 345L);
    }

    @Test
    public void testGetCell() {
        when(cells.lookup(42L)).thenReturn(Collections.singletonList(cell));
        Assert.assertEquals(SUT.getCell(42L), cell);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetCellEmpty() {
        when(cells.lookup(42L)).thenReturn(Collections.emptyList());
        SUT.getCell(42L);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetCellMultiple() {
        when(cells.lookup(42L)).thenReturn(Arrays.asList(cell, new Cell("other", null)));
        SUT.getCell(42L);
    }
}
