
package org.openrefine.model.changes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import scala.Tuple2;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.overlay.OverlayModel;

/**
 * A change which acts by transforming each row regardless of its context, and only those matched by facets. In records
 * mode, this change is applied on each row of the filtered records.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class RowMapChange extends EngineDependentChange {

    /**
     * Constructs a change given a row-wise function to apply to all filtered rows.
     * 
     * @param engineConfig
     *            the facets and engine mode to determine the filtered rows
     */
    public RowMapChange(EngineConfig engineConfig) {
        super(engineConfig);
    }

    /**
     * Returns the function that is applied to each row and row index.
     * 
     * @param columnModel
     *            the initial column model
     */
    public abstract Function2<Long, Row, Row> getRowMap(ColumnModel columnModel);

    /**
     * Subclasses can override this to change the column model when the change is applied
     * 
     * @param grid
     *            the initial grid state
     * @return the new column model
     */
    public ColumnModel getNewColumnModel(GridState grid) {
        return grid.getColumnModel();
    }

    /**
     * Subclasses can override this to change the overlay models when the change is applied.
     * 
     * @param grid
     *            the initial grid state
     * @return the new column model
     */
    public Map<String, OverlayModel> getNewOverlayModels(GridState grid) {
        return grid.getOverlayModels();
    }

    @Override
    public GridState apply(GridState projectState) {
        Engine engine = getEngine(projectState);
        Function2<Long, Row, Row> operation = getRowMap(projectState.getColumnModel());
        JavaPairRDD<Long, Row> rows;
        if (Mode.RowBased.equals(engine.getMode())) {
            RowFilter rowFilter = engine.combinedRowFilters();
            rows = GridState.mapKeyValuesToValues(projectState.getGrid(),
                    (Long idx, Row row) -> rowFilter.filterRow(idx, row) ? operation.call(idx, row) : row);
        } else {
            // records mode
            RecordFilter recordFilter = engine.combinedRecordFilters();
            rows = JavaPairRDD.fromJavaRDD((projectState.getRecords().flatMapValues(
                    recordToRows(operation, recordFilter)).values()));
        }
        return new GridState(getNewColumnModel(projectState), rows, getNewOverlayModels(projectState));
    }

    private static Function<Record, Iterable<Tuple2<Long, Row>>> recordToRows(Function2<Long, Row, Row> operation,
            RecordFilter recordFilter) {
        return new Function<Record, Iterable<Tuple2<Long, Row>>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterable<Tuple2<Long, Row>> call(Record record) throws Exception {
                List<Row> rows = record.getRows();
                Stream<Tuple2<Long, Row>> stream = IntStream.range(0, rows.size())
                        .mapToObj(idx -> new Tuple2<Long, Row>(record.getStartRowId() + idx, rows.get(idx)));
                if (recordFilter.filterRecord(record)) {
                    stream = stream.map(tuple -> {
                        try {
                            return new Tuple2<Long, Row>(tuple._1, operation.call(tuple._1, tuple._2));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                return stream.collect(Collectors.toList());
            }

        };
    }

}
