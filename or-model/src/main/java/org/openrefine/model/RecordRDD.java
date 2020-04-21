
package org.openrefine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.spark.Partition;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.rdd.RDD;
import scala.Function2;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

import org.openrefine.expr.ExpressionUtils;

/**
 * A RDD of Records which is efficiently computed from the corresponding RDD of indexed rows. Rows are grouped into
 * records, even when the records cross partition boundaries. This grouping is also stable: turning a RDD of records
 * into a RDD of rows and then grouping the rows into records again gives the same partitioning.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RecordRDD extends RDD<Record> implements Serializable {

    private static final long serialVersionUID = -159614854421148220L;
    private static final ClassTag<Record> RECORD_TAG = ClassManifestFactory$.MODULE$.fromClass(Record.class);
    private static final ClassTag<UnfinishedRecord> UNFINISHED_RECORD_TAG = ClassManifestFactory$.MODULE$.fromClass(UnfinishedRecord.class);

    /**
     * A partition holds a pointer to the source partition, and a list of additional rows which should be added at the
     * end of the partition to complete the last record which starts in this partition.
     */
    protected static class RecordRDDPartition implements Partition, Serializable {

        private static final long serialVersionUID = -4946262934177869432L;
        private final Partition prev;
        private final List<Row> additionalRows;

        protected RecordRDDPartition(Partition prev, List<Row> additionalRows) {
            this.prev = prev;
            this.additionalRows = additionalRows;
        }

        @Override
        public int index() {
            return prev.index();
        }

    }

    /**
     * The last few rows of a record, located at the beginning of a partition.
     */
    protected static class UnfinishedRecord implements Serializable {

        private static final long serialVersionUID = -6940320543502333246L;
        // the last rows of the record
        protected List<Row> rows;
        // whether the rows above were stopped by a new record key
        // (if not, we reached the end of the partition)
        protected boolean recordStartFound;

        protected UnfinishedRecord(List<Row> rows, boolean recordStartFound) {
            this.rows = rows;
            this.recordStartFound = recordStartFound;
        }
    }

    private final ClassTag<Tuple2<Long, Row>> classTag;
    private final int keyCellIndex;
    private final UnfinishedRecord[] firstRows;

    /**
     * Creates a RDD of records from a RDD of rows.
     * 
     * @param prev
     *            the RDD of indexed rows, where row ids are assumed to be sorted in increasing order
     * @param keyCellIndex
     *            the column index used to determine record boundaries
     */
    public RecordRDD(JavaPairRDD<Long, Row> prev, int keyCellIndex) {
        super(prev.rdd(), RECORD_TAG);
        classTag = prev.classTag();
        this.keyCellIndex = keyCellIndex;
        List<Object> partitionIds = new ArrayList<>(prev.getNumPartitions() - 1);
        for (int i = 1; i != prev.getNumPartitions(); i++) {
            partitionIds.add(i);
        }
        Seq<Object> partitionIdObjs = JavaConverters.collectionAsScalaIterable(partitionIds).toSeq();
        firstRows = (UnfinishedRecord[]) prev.context().runJob(prev.rdd(), new ExtractFirstRecord(keyCellIndex), partitionIdObjs,
                UNFINISHED_RECORD_TAG);
    }

    @Override
    public Iterator<Record> compute(Partition partition, TaskContext context) {
        RecordRDDPartition recordPartition = (RecordRDDPartition) partition;
        Iterator<Tuple2<Long, Row>> parentIter = this.firstParent(classTag).iterator(recordPartition.prev, context);
        return new Iterator<Record>() {

            Tuple2<Long, Row> fetchedRowTuple = null;
            Record nextRecord = null;
            boolean additionalRowsConsumed = false;
            boolean firstRowsIgnored = recordPartition.index() == 0;

            @Override
            public boolean hasNext() {
                buildNextRecord();
                if (firstRowsIgnored && nextRecord != null) {
                    return true;
                }
                firstRowsIgnored = true;
                buildNextRecord();
                return nextRecord != null;
            }

            @Override
            public Record next() {
                return nextRecord;
            }

            private void buildNextRecord() {
                List<Row> rows = new ArrayList<>();
                long startRowId = 0;
                if (fetchedRowTuple != null) {
                    rows.add(fetchedRowTuple._2);
                    startRowId = fetchedRowTuple._1;
                    fetchedRowTuple = null;
                }
                while (parentIter.hasNext()) {
                    fetchedRowTuple = parentIter.next();
                    Row row = fetchedRowTuple._2;
                    if (ExpressionUtils.isNonBlankData(row.getCellValue(keyCellIndex))) {
                        break;
                    }
                    rows.add(row);
                    fetchedRowTuple = null;
                }
                if (!parentIter.hasNext() && fetchedRowTuple == null && !additionalRowsConsumed) {
                    rows.addAll(recordPartition.additionalRows);
                    additionalRowsConsumed = true;
                }
                nextRecord = rows.isEmpty() ? null : new Record(startRowId, rows);
            }

        };
    }

    @Override
    public Partition[] getPartitions() {
        Partition[] origPartitions = this.firstParent(classTag).getPartitions();
        Partition[] newPartitions = new Partition[origPartitions.length];

        for (int i = 0; i != origPartitions.length; i++) {
            List<Row> additionalRows = Collections.emptyList();
            if (i < origPartitions.length - 1) {
                if (firstRows[i].recordStartFound) {
                    additionalRows = firstRows[i].rows;
                } else {
                    // no record start was found in the entire partition,
                    // so we need to fetch rows from the following partitions as well
                    additionalRows = new ArrayList<>();
                    for (int j = i; j < origPartitions.length - 1; j++) {
                        additionalRows.addAll(firstRows[j].rows);
                        if (firstRows[j].recordStartFound) {
                            break;
                        }
                    }
                }

            }
            newPartitions[i] = new RecordRDDPartition(origPartitions[i], additionalRows);

        }

        return newPartitions;
    }

    /**
     * Extracts the first rows of a partition until a row with a non-blank value in the record key column is
     * encountered. This can fetch the entire partition if no record key is found.
     * 
     * @author Antonin Delpeuch
     *
     */
    public static class ExtractFirstRecord implements Function2<TaskContext, Iterator<Tuple2<Long, Row>>, UnfinishedRecord>, Serializable {

        private static final long serialVersionUID = 8473670054764783718L;
        private final int keyCellIndex;

        ExtractFirstRecord(int keyCellIndex) {
            this.keyCellIndex = keyCellIndex;
        }

        @Override
        public UnfinishedRecord apply(TaskContext v1, Iterator<Tuple2<Long, Row>> iterator) {
            List<Row> currentRows = new ArrayList<>();
            boolean recordStartFound = false;
            while (!recordStartFound && iterator.hasNext()) {
                Tuple2<Long, Row> tuple = iterator.next();
                if (ExpressionUtils.isNonBlankData(tuple._2.getCellValue(keyCellIndex))) {
                    recordStartFound = true;
                } else {
                    currentRows.add(tuple._2);
                }
            }
            return new UnfinishedRecord(currentRows, recordStartFound);
        }
    }

}
