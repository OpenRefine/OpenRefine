
package org.openrefine.model.rdd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;
import org.apache.spark.Partition;
import org.apache.spark.Partitioner;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.rdd.RDD;
import scala.Function2;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassManifestFactory;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * A RDD of Records which is efficiently computed from the corresponding RDD of indexed rows. Rows are grouped into
 * records, even when the records cross partition boundaries. This grouping is also stable: turning a RDD of records
 * into a RDD of rows with the same partitioner, and then grouping the rows into records again gives the same
 * partitioner. This last property ensures that there is no drift of rows across partitions as the grid gets
 * grouped/ungrouped into records repeatedly.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RecordRDD extends RDD<Tuple2<Long, Record>> implements Serializable {

    private static final long serialVersionUID = -159614854421148220L;
    @SuppressWarnings("unchecked")
    private static final ClassTag<Tuple2<Long, Record>> TUPLE2_TAG = ClassManifestFactory
            .fromClass((Class<Tuple2<Long, Record>>) (Class<?>) Tuple2.class);
    private static final ClassTag<Record> RECORD_TAG = ClassManifestFactory$.MODULE$.fromClass(Record.class);
    private static final ClassTag<Long> LONG_TAG = ClassManifestFactory$.MODULE$.fromClass(Long.class);
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
        // the row id of the first row starting a new record
        // (if null, we reached the end of the partition without finding one)
        protected Long firstRecordStart;

        protected UnfinishedRecord(List<Row> rows, Long firstRecordStart) {
            this.rows = rows;
            this.firstRecordStart = firstRecordStart;
        }
    }

    private final ClassTag<Tuple2<Long, Row>> classTag;
    private final int keyCellIndex;
    private final UnfinishedRecord[] firstRows;
    private final Partitioner sortedPartitioner;

    /**
     * Creates a RDD of records from a RDD of rows.
     * 
     * @param prev
     *            the RDD of indexed rows, where row ids are assumed to be sorted in increasing order
     * @param keyCellIndex
     *            the column index used to determine record boundaries
     */
    public RecordRDD(JavaPairRDD<Long, Row> prev, int keyCellIndex) {
        super(prev.rdd(), TUPLE2_TAG);
        classTag = prev.classTag();
        this.keyCellIndex = keyCellIndex;
        List<Object> partitionIds = new ArrayList<>(prev.getNumPartitions() - 1);
        for (int i = 1; i != prev.getNumPartitions(); i++) {
            partitionIds.add(i);
        }
        Seq<Object> partitionIdObjs = JavaConverters.collectionAsScalaIterable(partitionIds).toSeq();
        firstRows = (UnfinishedRecord[]) prev.context().runJob(prev.rdd(), new ExtractFirstRecord(keyCellIndex), partitionIdObjs,
                UNFINISHED_RECORD_TAG);
        sortedPartitioner = new SortedRDD.SortedPartitioner<Long>(prev.getNumPartitions(),
                Arrays.asList(firstRows).stream().map(ur -> ur.firstRecordStart).collect(Collectors.toList()));
    }

    /**
     * @return the same RDD with the Java API
     */
    public JavaPairRDD<Long, Record> toJavaPairRDD() {
        return new JavaPairRDD<Long, Record>(this, LONG_TAG, RECORD_TAG);
    }

    @Override
    public Option<Partitioner> partitioner() {
        return Option.apply(sortedPartitioner);
    }

    @Override
    public Iterator<Tuple2<Long, Record>> compute(Partition partition, TaskContext context) {
        RecordRDDPartition recordPartition = (RecordRDDPartition) partition;
        Iterator<Tuple2<Long, Row>> parentIter = this.firstParent(classTag).iterator(recordPartition.prev, context);
        java.util.Iterator<IndexedRow> indexedRows = Iterators.transform(
                JavaConverters.asJavaIterator(parentIter),
                tuple -> new IndexedRow(tuple._1, tuple._2));
        java.util.Iterator<Record> records = Record.groupIntoRecords(
                indexedRows,
                keyCellIndex,
                recordPartition.index() != 0,
                recordPartition.additionalRows);
        return JavaConverters.asScalaIterator(
                Iterators.transform(records, record -> new Tuple2<Long, Record>(record.getStartRowId(), record)));
    }

    @Override
    public Partition[] getPartitions() {
        Partition[] origPartitions = this.firstParent(classTag).getPartitions();
        Partition[] newPartitions = new Partition[origPartitions.length];

        for (int i = 0; i != origPartitions.length; i++) {
            List<Row> additionalRows = Collections.emptyList();
            if (i < origPartitions.length - 1 && !(i > 0 && firstRows[i - 1].firstRecordStart == null)) {
                if (firstRows[i].firstRecordStart != null) {
                    additionalRows = firstRows[i].rows;
                } else {
                    // no record start was found in the entire partition,
                    // so we need to fetch rows from the following partitions as well
                    additionalRows = new ArrayList<>();
                    for (int j = i; j < origPartitions.length - 1; j++) {
                        additionalRows.addAll(firstRows[j].rows);
                        if (firstRows[j].firstRecordStart != null) {
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
    protected static class ExtractFirstRecord
            implements Function2<TaskContext, Iterator<Tuple2<Long, Row>>, UnfinishedRecord>, Serializable {

        private static final long serialVersionUID = 8473670054764783718L;
        private final int keyCellIndex;

        ExtractFirstRecord(int keyCellIndex) {
            this.keyCellIndex = keyCellIndex;
        }

        @Override
        public UnfinishedRecord apply(TaskContext v1, Iterator<Tuple2<Long, Row>> iterator) {
            List<Row> currentRows = new ArrayList<>();
            Long firstRecordStart = null;
            while (firstRecordStart == null && iterator.hasNext()) {
                Tuple2<Long, Row> tuple = iterator.next();
                if (Record.isRecordStart(tuple._2, keyCellIndex)) {
                    firstRecordStart = tuple._1;
                } else {
                    currentRows.add(tuple._2);
                }
            }
            return new UnfinishedRecord(currentRows, firstRecordStart);
        }
    }

}
