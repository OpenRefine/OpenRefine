
package org.openrefine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RecordTests {

    List<Row> rows;
    Record SUT;

    @BeforeMethod
    public void setUp() {
        rows = Arrays.asList(
                row("a", "b"),
                row(null, "c"),
                row("", "d"));
        SUT = new Record(34L, rows);
    }

    @Test
    public void testAccessors() {
        Assert.assertEquals(SUT.getRows(), rows);
        Assert.assertEquals(SUT.getStartRowId(), 34L);
        Assert.assertEquals(SUT.getEndRowId(), 37L);
        Assert.assertEquals(SUT.size(), 3);
    }

    @Test
    public void testIsRecordStart() {
        Assert.assertTrue(Record.isRecordStart(rows.get(0), 0));
        Assert.assertFalse(Record.isRecordStart(rows.get(1), 0));
        Assert.assertFalse(Record.isRecordStart(rows.get(2), 0));
    }

    @Test
    public void testGroupRowsIntoRecords() {
        List<Record> records = groupRows(
                row(null, "z"),
                row("a", "b"),
                row("", "c"),
                row(null, null),
                row("", "d"),
                row("e", "f"),
                row(null, "g"));

        List<Record> expected = Arrays.asList(
                record(1L, row("a", "b"), row("", "c")),
                record(3L, row(null, null), row("", "d")),
                record(5L, row("e", "f"), row(null, "g")));

        Assert.assertEquals(records, expected);
    }

    protected List<Record> groupRows(Row... rows) {
        Iterator<IndexedRow> indexedRows = IntStream.range(0, rows.length).mapToObj(i -> new IndexedRow(i, rows[i])).iterator();
        Iterator<Record> records = Record.groupIntoRecords(indexedRows, 0, true, Collections.emptyList());
        List<Record> list = new ArrayList<>();
        while (records.hasNext()) {
            list.add(records.next());
        }
        return list;
    }

    protected Row row(Serializable... values) {
        return new Row(
                Arrays.asList(values).stream().map(v -> v == null ? null : new Cell(v, null)).collect(Collectors.toList()));
    }

    protected Record record(long startId, Row... rows) {
        return new Record(startId, Arrays.asList(rows));
    }

    @Test
    public void testEquals() {
        Assert.assertNotEquals(SUT, 23);
        Assert.assertNotEquals(SUT, new Record(34L, Collections.emptyList()));
        Assert.assertEquals(SUT, new Record(34L, rows));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(SUT.hashCode(), new Record(34L, rows).hashCode());
    }

    @Test
    public void testToString() {
        Assert.assertTrue(SUT.toString().contains("Record"));
    }

}
