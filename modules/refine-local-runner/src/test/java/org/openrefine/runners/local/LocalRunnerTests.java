
package org.openrefine.runners.local;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.*;
import org.openrefine.runners.testing.RunnerTestBase;
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
        Assert.assertEquals(initial.recordCount(), 3L);

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
        Assert.assertEquals(mapped.cachedRecordCount, 3L);
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
            public List<Row> call(Record record, String changeData) {
                return record.getRows().stream()
                        .map(r -> r.withCell(1, new Cell(changeData, null)))
                        .collect(Collectors.toList());
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }
        };

        ChangeData<String> changeData = getDatamodelRunner().create(Arrays.asList(
                new IndexedData(0L, "foo")));
        LocalGrid joined = (LocalGrid) initial.join(changeData, joiner, initial.getColumnModel());
        Assert.assertFalse(joined.constructedFromRows);
    }

    @Test
    public void testMemoryCostPrediction() throws Change.DoesNotApplyException {
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

        ChangeData<JsonNode> changeData = getDatamodelRunner().loadChangeData(tempFile, serializer);

        Assert.assertFalse(changeData.isComplete());
        Assert.assertEquals(changeData.get(34L), indexedDataList.get(0).getData());
        Assert.assertNull(changeData.get(56L));
    }

}
