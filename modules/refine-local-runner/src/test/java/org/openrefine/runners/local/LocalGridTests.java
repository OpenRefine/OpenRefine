
package org.openrefine.runners.local;

import static org.openrefine.runners.local.LocalGrid.applyRowChangeDataMapperWithIncompleteData;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.*;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.util.CloseableIterator;

/*
 * Most of the tests for LocalGrid are actually done in LocalRunnerTests as part of the general
 * test suite for runners. The test cases here are only about the specific behaviour of the local runner.
 */
public class LocalGridTests {

    @Test
    public void testBatchFetchWithNull() {

        RowChangeDataProducer<String> rowMapper = new RowChangeDataProducer<String>() {

            @Override
            public String call(long rowId, Row row, ColumnModel columnModel) {
                return null;
            }
        };

        ColumnModel columnModel = new ColumnModel(Collections.singletonList(new ColumnMetadata("column")));
        List<Tuple2<Long, Tuple2<IndexedRow, IndexedData<String>>>> batch = Arrays.asList(
                Tuple2.of(3L, Tuple2.of(new IndexedRow(3L, new Row(Collections.singletonList(null))), new IndexedData<>(3L, "foo"))),
                Tuple2.of(4L, Tuple2.of(new IndexedRow(4L, new Row(Collections.singletonList(null))), new IndexedData<>(4L, null))));
        List<Tuple2<Long, IndexedData<String>>> expected = Arrays.asList(
                Tuple2.of(3L, new IndexedData<>(3L, "foo")),
                Tuple2.of(4L, new IndexedData<>(4L, null)));
        ColumnMapper columnMapper = new ColumnMapper(Collections.singletonList(new ColumnId("column", 0L)), columnModel);

        CloseableIterator<Tuple2<Long, IndexedData<String>>> result = applyRowChangeDataMapperWithIncompleteData(
                rowMapper, columnMapper, batch, columnModel);

        List<Tuple2<Long, IndexedData<String>>> collected = result.collect(Collectors.toList());
        Assert.assertEquals(collected, expected);
    }
}
