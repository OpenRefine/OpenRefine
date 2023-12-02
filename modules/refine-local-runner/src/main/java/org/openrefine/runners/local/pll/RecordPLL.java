
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import io.vavr.collection.Array;

import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.runners.local.pll.util.IterationContext;
import org.openrefine.util.CloseableIterator;

/**
 * A PLL of records efficiently computed from the underlying PLL of rows.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RecordPLL extends PLL<Tuple2<Long, Record>> {

    private final PairPLL<Long, IndexedRow> parent;
    private Array<RecordPartition> partitions;
    protected final int keyColumnIndex;

    /**
     * Constructs an indexed PLL of records by grouping rows together. Any partitioner on the parent PLL will be used on
     * the resulting pair PLL.
     * 
     * @param grid
     *            the PLL of rows
     * @param keyColumnIndex
     *            the index of the column used as record key
     */
    public static PairPLL<Long, Record> groupIntoRecords(PairPLL<Long, IndexedRow> grid, int keyColumnIndex) {
        return new PairPLL<Long, Record>(new RecordPLL(grid, keyColumnIndex), grid.getPartitioner());
    }

    /**
     * Constructs a PLL of records by grouping rows together. Any partitioner on the parent PLL can be used to partition
     * this resulting PLL.
     * 
     * @param grid
     *            the PLL of rows
     * @param keyColumnIndex
     *            the index of the column used as record key
     */
    public RecordPLL(PairPLL<Long, IndexedRow> grid, int keyColumnIndex) {
        super(grid.getContext(), "Group into records");
        this.keyColumnIndex = keyColumnIndex;
        Array<? extends Partition> parentPartitions = grid.getPartitions();
        parent = grid;
        List<RecordPartition> partitions = new ArrayList<>(parentPartitions.size());
        for (int i = 0; i != parentPartitions.size(); i++) {
            partitions.add(new RecordPartition(i, parentPartitions.get(i),
                    i == parentPartitions.size() - 1 ? null : parentPartitions.get(i + 1)));
        }
        this.partitions = Array.ofAll(partitions);
    }

    protected static RecordEnd extractRecordEnd(CloseableIterator<IndexedRow> iterator, int keyColumnIndex) {
        try (iterator) {
            // We cannot use Stream.takeWhile here because we need to know if we have reached the end of the stream
            List<Row> end = new ArrayList<>();
            IndexedRow lastTuple = null;
            while (iterator.hasNext()) {
                lastTuple = iterator.next();
                if (!lastTuple.getRow().isCellBlank(keyColumnIndex)) {
                    break;
                }
                end.add(lastTuple.getRow());
            }
            return new RecordEnd(end, lastTuple == null || lastTuple.getRow().isCellBlank(keyColumnIndex));
        }
    }

    protected static CloseableIterator<Tuple2<Long, Record>> groupIntoRecords(
            CloseableIterator<IndexedRow> indexedRows,
            int keyCellIndex,
            boolean ignoreFirstRows,
            Supplier<CloseableIterator<Row>> additionalRows) {
        CloseableIterator<Record> recordIterator = Record.groupIntoRecords(indexedRows, keyCellIndex, ignoreFirstRows, additionalRows);
        return recordIterator.map(
                record -> Tuple2.of(record.getStartRowId(), record));
    }

    @Override
    protected CloseableIterator<Tuple2<Long, Record>> compute(Partition partition, IterationContext context) {
        RecordPartition recordPartition = (RecordPartition) partition;
        CloseableIterator<IndexedRow> rows = parent.iterate(recordPartition.getParent(), context)
                .map(Tuple2::getValue);

        return groupIntoRecords(rows, keyColumnIndex, partition.getIndex() != 0, () -> {
            if (recordPartition.getNextParent() == null) {
                return CloseableIterator.empty();
            } else {
                return parent.iterateFromPartition(recordPartition.getNextParent().getIndex(), context)
                        .map(tuple -> tuple.getValue().getRow());
            }
        });
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.singletonList(parent);
    }

    protected static class RecordPartition implements Partition {

        protected final int index;
        protected final Partition parent;
        protected final Partition nextParent;

        /**
         * Constructs a partition of records.
         * 
         * @param index
         *            the index of the partition in the records PLL
         * @param parent
         *            the parent partition in the rows PLL
         * @param nextParent
         *            the following partition in the rows PLL, or null if there isn't
         */
        protected RecordPartition(int index, Partition parent, Partition nextParent) {
            this.index = index;
            this.parent = parent;
            this.nextParent = nextParent;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Partition getParent() {
            return parent;
        }

        public Partition getNextParent() {
            return this.nextParent;
        }
    }

    // The last few rows of a record, at the beginning of a partition
    protected static class RecordEnd {

        // the last rows of the record
        protected List<Row> rows;
        // whether this list of rows actually spans the entire partition
        protected boolean partitionExhausted;

        protected RecordEnd(List<Row> rows, boolean partitionExhausted) {
            this.rows = rows;
            this.partitionExhausted = partitionExhausted;
        }
    }

}
