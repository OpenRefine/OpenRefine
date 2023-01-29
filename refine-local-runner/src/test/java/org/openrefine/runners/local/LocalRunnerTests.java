
package org.openrefine.runners.local;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.Change;
import org.openrefine.runners.local.LocalGrid;
import org.openrefine.runners.local.LocalRunner;

/**
 * Tests for this datamodel implementation are taken from the standard test suite, in {@link RunnerTestBase}.
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
    public void testMemoryCostPrediction() throws Change.DoesNotApplyException {
        LocalGrid smallGrid = (LocalGrid) createGrid(new String[] { "foo" }, new Serializable[][] {});

        // caching a small grid should always be possible
        assertTrue(smallGrid.smallEnoughToCacheInMemory());
    }

}
