
package org.openrefine.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for this datamodel implementation are taken from the standard test suite, in {@link DatamodelRunnerTestBase}.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LocalDatamodelRunnerTests extends DatamodelRunnerTestBase {

    @Override
    public DatamodelRunner getDatamodelRunner() throws IOException {
        Map<String, String> map = new HashMap<>();
        // these values are purposely very low for testing purposes,
        // so that we can check the partitioning strategy without using large files
        map.put("minSplitSize", "128");
        map.put("maxSplitSize", "1024");

        RunnerConfiguration runnerConf = new RunnerConfigurationImpl(map);
        return new LocalDatamodelRunner(runnerConf);
    }

    @Test
    public void testRecordPreservation() {
        GridState initial = createGrid(new String[] { "key", "values" },
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

        LocalGridState first = (LocalGridState) initial.mapRecords(mapper, initial.getColumnModel());
        LocalGridState second = (LocalGridState) first.mapRecords(mapper, initial.getColumnModel());
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
        LocalGridState third = (LocalGridState) second.withOverlayModels(Collections.emptyMap());
        Assert.assertFalse(third.constructedFromRows);
        // changing the column model does not either
        LocalGridState fourth = (LocalGridState) third.withColumnModel(initial.getColumnModel());
        Assert.assertFalse(fourth.constructedFromRows);
    }

}
