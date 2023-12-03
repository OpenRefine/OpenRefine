
package org.openrefine.runners.local;

import static org.openrefine.runners.local.LocalRunner.fillWithIncompleteIndexedData;
import static org.testng.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.*;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.runners.local.pll.PLL;
import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.runners.testing.RunnerTestBase;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.ParsingUtilities;

/**
 * Tests for this datamodel implementation are taken from the standard test suite, in {@link RunnerTestBase}. Some
 * additional tests are provided here: those test some optimizations that are specific to the local runner.
 *
 * @author Antonin Delpeuch
 *
 */
public class LocalRunnerTests extends RunnerTestBase {

    @Override
    public Runner getDatamodelRunner() throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("defaultParallelism", "4");
        // these values are purposely very low for testing purposes,
        // so that we can check the partitioning strategy without using large files
        map.put("minSplitSize", "128");
        map.put("maxSplitSize", "1024");

        RunnerConfiguration runnerConf = new RunnerConfigurationImpl(map);
        return new LocalRunner(runnerConf);
    }

    @Test
    public void testRecordPreservation() {
        Grid initial = createGrid(new String[] { "key", "values" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 2 },
                        { "b", 3 },
                        { null, 4 },
                        { null, 5 },
                        { "c", 6 }
                });

        RecordMapper mapper = new RecordMapper() {

            @Override
            public List<Row> call(Record record) {
                return record.getRows().stream()
                        .map(r -> r.withCell(1, new Cell((int) r.getCell(1).getValue() * 2, null)))
                        .collect(Collectors.toList());
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }
        };

        LocalGrid first = (LocalGrid) initial.mapRecords(mapper, initial.getColumnModel());
        LocalGrid second = (LocalGrid) first.mapRecords(mapper, initial.getColumnModel());
        Assert.assertFalse(first.constructedFromRows);
        Assert.assertFalse(second.constructedFromRows);
        // the query plan for the rows contains a flattening of the records, because those rows were derived from
        // records
        String rowsQueryTree = second.getRowsQueryTree().toString();
        Assert.assertTrue(rowsQueryTree.contains("flatten records to rows"));
        // the query plan for records does not contain any flattening, not even between the first and second states,
        // because records were preserved.
        String recordsQueryTree = second.getRecordsQueryTree().toString();
        Assert.assertFalse(recordsQueryTree.contains("flatten records to rows"));

        // changing the overlay models does not convert to rows
        LocalGrid third = (LocalGrid) second.withOverlayModels(Collections.emptyMap());
        Assert.assertFalse(third.constructedFromRows);
        // changing the column model does not either
        LocalGrid fourth = (LocalGrid) third.withColumnModel(initial.getColumnModel());
        Assert.assertFalse(fourth.constructedFromRows);
    }

    @Test
    public void testRecordCountPreservation() {
        Grid initial = createGrid(new String[] { "key", "values" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 2 },
                        { "b", 3 },
                        { null, 4 },
                        { null, 5 },
                        { "c", 6 }
                });

        // compute the record count on the initial grid
        assertEquals(initial.recordCount(), 3L);

        RowMapper mapper = new RowMapper() {

            @Override
            public Row call(long rowId, Row row) {
                return row.withCell(1, new Cell("constant", null));
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }
        };

        // apply a row mapper that preserves records
        LocalGrid mapped = (LocalGrid) initial.mapRows(mapper, initial.getColumnModel());

        // check that the number of records is already cached and does not need recomputing
        assertEquals(mapped.cachedRecordCount, 3L);
    }

    @Test
    public void testRecordPreservationForChangeDataJoining() throws IOException {
        Grid initial = createGrid(new String[] { "key", "values" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 2 },
                        { "b", 3 },
                        { null, 4 },
                        { null, 5 },
                        { "c", 6 }
                });

        RecordChangeDataJoiner<String> joiner = new RecordChangeDataJoiner<>() {

            @Override
            public List<Row> call(Record record, IndexedData<String> changeData) {
                return record.getRows().stream()
                        .map(r -> r.withCell(1, new Cell(changeData.getData(), null, changeData.isPending())))
                        .collect(Collectors.toList());
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }
        };

        ChangeData<String> changeData = getDatamodelRunner().changeDataFromList(Arrays.asList(
                new IndexedData<String>(0L, "foo")));
        LocalGrid joined = (LocalGrid) initial.join(changeData, joiner, initial.getColumnModel());
        Assert.assertFalse(joined.constructedFromRows);
    }

    @Test
    public void testMemoryCostPrediction() throws OperationException {
        LocalGrid smallGrid = (LocalGrid) createGrid(new String[] { "foo" }, new Serializable[][] {});

        // caching a small grid should always be possible
        assertTrue(smallGrid.smallEnoughToCacheInMemory());
    }

    @Test
    public void testParseIncompleteChangeData() throws IOException {
        List<IndexedData<JsonNode>> indexedDataList = Collections.singletonList(
                new IndexedData<>(34L, ParsingUtilities.mapper.readTree("{\"foo\":2}")));
        ChangeDataSerializer<JsonNode> serializer = new ChangeDataSerializer<JsonNode>() {

            @Override
            public String serialize(JsonNode changeDataItem) {
                try {
                    return ParsingUtilities.mapper.writeValueAsString(changeDataItem);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public JsonNode deserialize(String serialized) throws IOException {
                return ParsingUtilities.mapper.readTree(serialized);
            }
        };

        // set up a truncated test file, where the JSON serialization of a record abruptly stops
        File tempFile = new File(tempDir, "incomplete_changedata_1");
        tempFile.mkdir();
        File changeDataFile = new File(tempFile, "part-00000");
        try (FileWriter writer = new FileWriter(changeDataFile)) {
            // the first line is written out fine
            writer.write(indexedDataList.get(0).writeAsString(serializer) + "\n");
            // the second is interrupted abruptly
            writer.write("56,{\"some unfinished json");
        }

        ChangeData<JsonNode> changeData = getDatamodelRunner().loadChangeData(tempFile, serializer, false);

        Assert.assertFalse(changeData.isComplete());
        assertEquals(changeData.get(34L), indexedDataList.get(0));
        assertEquals(changeData.get(56L), new IndexedData<>(56L));
    }

    @Test
    public void testSampleOnManyPartitions() throws IOException {
        LocalRunner runner = (LocalRunner) getDatamodelRunner();
        assertEquals(runner.defaultParallelism, 4);

        /*
         * When a PLL has many partitions (here, 16), we do not want to sample from every single partition because it
         * would mean opening a lot of files to only process few rows in it.
         *
         * In this toy example we only open 8 of them, equally spread in the collection, and process 4 rows in each of
         * them.
         */

        PLL<IndexedRow> longPLL = runner.getPLLContext().parallelize(16, IntStream.range(0, 128)
                .mapToObj(i -> new IndexedRow((long) i, new Row(List.of(new Cell(i, null))))).collect(Collectors.toList()));
        ColumnModel columnModel = new ColumnModel(Arrays.asList(new ColumnMetadata("foo")));
        LocalGrid localGrid = new LocalGrid(runner, columnModel, longPLL.zipWithIndex(), Map.of(), 64L);
        Grid.PartialAggregation<String> result = localGrid.sample(longPLL, 32L, "",
                (s, row) -> String.format("%s_%d", s, (int) row.getRow().getCellValue(0)),
                (s1, s2) -> String.format("%s/%s", s1, s2));

        assertEquals(result.getState(),
                "/_0_1_2_3/_16_17_18_19/_32_33_34_35/_48_49_50_51/_64_65_66_67/_80_81_82_83/_96_97_98_99/_112_113_114_115");
        Assert.assertTrue(result.limitReached());
        assertEquals(result.getProcessed(), 32L);
    }

    @Test
    public void testUnionWithCachedRecordCount() {
        Grid first = createGrid(new String[] { "key", "values" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 2 },
                        { "b", 3 },
                        { null, 4 },
                        { null, 5 },
                        { "c", 6 }
                });
        // manually cache the record count
        assertEquals(first.recordCount(), 3L);
        assertEquals(((LocalGrid) first).cachedRecordCount, 3L);
        Grid second = createGrid(new String[] { "key", "values" },
                new Serializable[][] {
                        { "", 1 },
                        { null, 2 },
                        { "b", 3 },
                        { null, 4 },
                        { null, 5 },
                        { "c", 6 }
                });
        // manually cache the record count for this one too
        assertEquals(second.recordCount(), 3L);
        assertEquals(((LocalGrid) second).cachedRecordCount, 3L);

        Grid concatenated = first.concatenate(second);
        // the record count is already cached for the concatenation of both grids,
        // but is not equal to the sum because the last record of the first grid
        // extends to the second one.
        assertEquals(((LocalGrid) concatenated).cachedRecordCount, 5L);
    }

    @Test
    public void testUnionWithCachedRecordCountExactSum() {
        Grid first = createGrid(new String[] { "key", "values" },
                new Serializable[][] {
                        { "a", 1 },
                        { null, 2 },
                        { "b", 3 },
                        { null, 4 },
                        { null, 5 },
                        { "c", 6 }
                });
        // manually cache the record count
        assertEquals(first.recordCount(), 3L);
        assertEquals(((LocalGrid) first).cachedRecordCount, 3L);

        Grid concatenated = first.concatenate(first);
        assertEquals(((LocalGrid) concatenated).cachedRecordCount, 6L);
    }

    @Test
    public void testFillWithIncompleteChangeData() {
        CloseableIterator<Tuple2<Long, IndexedData<String>>> iterator = CloseableIterator.of(
                Tuple2.of(3L, new IndexedData<String>(3L, "foo")),
                Tuple2.of(5L, new IndexedData<String>(5L, "bar")));
        try (iterator) {
            CloseableIterator<Tuple2<Long, IndexedData<String>>> filled = fillWithIncompleteIndexedData(iterator, -1L, 8L);
            try (filled) {
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(3L, new IndexedData<String>(3L, "foo")));
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(5L, new IndexedData<String>(5L, "bar")));
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(6L, new IndexedData<String>(6L)));
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(7L, new IndexedData<String>(7L)));
                Assert.assertFalse(filled.hasNext());
            }
        }
    }

    @Test
    public void testFillWithIncompleteChangeDataEmpty() {
        CloseableIterator<Tuple2<Long, IndexedData<String>>> iterator = CloseableIterator.empty();
        try (iterator) {
            CloseableIterator<Tuple2<Long, IndexedData<String>>> filled = fillWithIncompleteIndexedData(iterator, -1L, 10L);
            try (filled) {
                Assert.assertFalse(filled.hasNext());
            }
        }
    }

    @Test
    public void testFillWithIncompleteChangeDataEmptyInitialPartition() {
        CloseableIterator<Tuple2<Long, IndexedData<String>>> iterator = CloseableIterator.empty();
        try (iterator) {
            CloseableIterator<Tuple2<Long, IndexedData<String>>> filled = fillWithIncompleteIndexedData(iterator, 0L, Long.MAX_VALUE);
            try (filled) {
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(0L, new IndexedData<String>(0L)));
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(1L, new IndexedData<String>(1L)));
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(2L, new IndexedData<String>(2L)));
                Assert.assertTrue(filled.hasNext());
                assertEquals(filled.next(), Tuple2.of(3L, new IndexedData<String>(3L)));
            }
        }
    }

    @Test
    public void testGenerateUpperBoundsFromFirstIndices() {
        List<Optional<Long>> firstIndices = Arrays.asList(
                Optional.empty(),
                Optional.of(5L),
                Optional.empty(),
                Optional.empty(),
                Optional.of(34L),
                Optional.of(78L));

        List<Long> expected = Arrays.asList(
                5L, 5L, 34L, 34L, 34L, 78L, Long.MAX_VALUE);

        assertEquals(LocalRunner.incompleteUpperBounds(firstIndices), expected);
    }

}
