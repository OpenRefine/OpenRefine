
package org.openrefine.model;

import org.openrefine.model.recon.ReconConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

public class ColumnMapperTests {

    ColumnModel columnModel;
    ReconConfig reconConfig;
    Row rowA = new Row(Arrays.asList(
            new Cell("a", null),
            new Cell("b", null),
            new Cell("d", null)));
    Row rowB = new Row(Arrays.asList(
            null,
            new Cell("g", null),
            new Cell("h", null)));
    IndexedRow indexedRowA = new IndexedRow(379L, rowA);
    IndexedRow indexedRowB = new IndexedRow(380L, rowB);

    Row emptyRow = new Row(Collections.emptyList());
    Row translatedRowA = new Row(
            Arrays.asList(new Cell("b", null), new Cell("a", null)));
    Row translatedRowB = new Row(
            Arrays.asList(new Cell("g", null), null));
    Record record = new Record(379L, Arrays.asList(rowA, rowB));
    Record translatedRecord = new Record(379L, Arrays.asList(translatedRowA, translatedRowB));

    ColumnMapper nullDepsSUT;
    ColumnMapper noDepsSUT;
    ColumnMapper twoDepsSUT;

    @BeforeMethod
    public void setUp() {
        reconConfig = mock(ReconConfig.class);
        columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("origFoo", "foo", 1234L, null),
                new ColumnMetadata("origBar", "bar", 5678L, reconConfig),
                new ColumnMetadata("origHello", "hello", 0L, null)),
                0, true);

        nullDepsSUT = new ColumnMapper(null, columnModel);
        noDepsSUT = new ColumnMapper(Collections.emptyList(), columnModel);
        twoDepsSUT = new ColumnMapper(Arrays.asList(
                new ColumnId("origBar", 5678L),
                new ColumnId("origFoo", 1234L),
                new ColumnId("origFoo", 1234L)), columnModel);
    }

    @Test
    public void testTranslateRow() {
        assertEquals(nullDepsSUT.translateRow(rowA), rowA);
        assertEquals(noDepsSUT.translateRow(rowA), emptyRow);
        assertEquals(twoDepsSUT.translateRow(rowA), translatedRowA);
    }

    @Test
    public void testTranslateIndexedRow() {
        assertEquals(nullDepsSUT.translateIndexedRow(indexedRowA), indexedRowA);
        assertEquals(noDepsSUT.translateIndexedRow(indexedRowA), new IndexedRow(379L, emptyRow));
        assertEquals(twoDepsSUT.translateIndexedRow(indexedRowA), new IndexedRow(379L, translatedRowA));
    }

    @Test
    public void testTranslateIndexedRowBatch() {
        List<IndexedRow> batch = Arrays.asList(indexedRowA, indexedRowB);

        assertEquals(nullDepsSUT.translateIndexedRowBatch(batch), batch);
        assertEquals(noDepsSUT.translateIndexedRowBatch(batch), Arrays.asList(
                new IndexedRow(379L, emptyRow),
                new IndexedRow(380L, emptyRow)));
        assertEquals(twoDepsSUT.translateIndexedRowBatch(batch), Arrays.asList(
                new IndexedRow(379L, translatedRowA),
                new IndexedRow(380L, translatedRowB)));
    }

    @Test
    public void testTranslateRecord() {
        assertEquals(nullDepsSUT.translateRecord(record), record);
        assertThrows(IllegalArgumentException.class, () -> noDepsSUT.translateRecord(record));
        assertEquals(twoDepsSUT.translateRecord(record), translatedRecord);
    }

    @Test
    public void testTranslateRecordBatch() {
        List<Record> batch = Collections.singletonList(record);

        assertEquals(nullDepsSUT.translateRecordBatch(batch), batch);
        assertThrows(IllegalArgumentException.class, () -> noDepsSUT.translateRecordBatch(batch));
        assertEquals(twoDepsSUT.translateRecordBatch(batch), Collections.singletonList(translatedRecord));
    }

    @Test
    public void testTranslateColumnIndices() {
        assertEquals(nullDepsSUT.translateColumnIndex(0), 0);
        assertEquals(nullDepsSUT.translateColumnIndex(1), 1);
        assertEquals(nullDepsSUT.translateColumnIndex(2), 2);

        assertEquals(noDepsSUT.translateColumnIndex(0), -1);
        assertEquals(noDepsSUT.translateColumnIndex(1), -1);
        assertEquals(noDepsSUT.translateColumnIndex(2), -1);

        assertEquals(twoDepsSUT.translateColumnIndex(0), 1);
        assertEquals(twoDepsSUT.translateColumnIndex(1), 0);
        assertEquals(twoDepsSUT.translateColumnIndex(2), -1);
    }

    @Test
    public void testGetReducedColumnModel() {
        assertEquals(nullDepsSUT.getReducedColumnModel(), columnModel);
        assertEquals(noDepsSUT.getReducedColumnModel(), new ColumnModel(Collections.emptyList(), -1, false));
        assertEquals(twoDepsSUT.getReducedColumnModel(), new ColumnModel(
                Arrays.asList(columnModel.getColumnByIndex(1),
                        columnModel.getColumnByIndex(0)),
                1, true));
    }

    @Test
    public void testGetDependencies() {
        assertNull(nullDepsSUT.getDependencies());
        assertEquals(noDepsSUT.getDependencies(), Collections.emptyList());
        assertEquals(twoDepsSUT.getDependencies(), Arrays.asList(
                new ColumnId("origBar", 5678L),
                new ColumnId("origFoo", 1234L)));
    }
}
