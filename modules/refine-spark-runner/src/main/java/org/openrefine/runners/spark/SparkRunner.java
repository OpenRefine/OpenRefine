
package org.openrefine.runners.spark;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.io.CompressionCodec;
import org.apache.spark.io.SnappyCompressionCodec;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.util.ShutdownHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function0;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import org.openrefine.ProjectManager;
import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.*;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.runners.spark.io.OrderedLocalFileSystem;
import org.openrefine.runners.spark.util.RDDUtils;
import org.openrefine.util.ParsingUtilities;

/**
 * Spark implementation of the data model.
 * 
 * @author Antonin Delpeuch
 *
 */
public class SparkRunner implements Runner {

    static final Logger logger = LoggerFactory.getLogger(SparkRunner.class);

    private JavaSparkContext context;
    private final int defaultParallelism;
    private final String sparkMasterURI;
    // used to return futures required by the Grid and ChangeData interfaces
    protected final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool());;

    public SparkRunner(RunnerConfiguration configuration) {
        this.defaultParallelism = configuration.getIntParameter("defaultParallelism", 4);
        this.sparkMasterURI = configuration.getParameter("sparkMasterURI", String.format("local[%d]", defaultParallelism));

        // set up Hadoop on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            try {
                System.setProperty("hadoop.home.dir", new File("server/target/lib/native/windows/hadoop").getCanonicalPath());
            } catch (IOException e) {
                logger.warn("unable to locate Windows Hadoop binaries, this will leave temporary files behind");
            }
        }
        SnappyCompressionCodec e;
        CompressionCodec t;

        context = new JavaSparkContext(
                new SparkConf()
                        .setAppName("OpenRefine")
                        .setMaster(sparkMasterURI));
        context.setLogLevel("WARN");
        context.hadoopConfiguration().set("fs.file.impl", OrderedLocalFileSystem.class.getName());

        // Set up hook to save projects when spark shuts down
        int priority = ShutdownHookManager.SPARK_CONTEXT_SHUTDOWN_PRIORITY() + 10;
        ShutdownHookManager.addShutdownHook(priority, sparkShutdownHook());
    }

    public SparkRunner(JavaSparkContext context) {
        this.context = context;
        this.defaultParallelism = context.defaultParallelism();
        this.sparkMasterURI = null;
    }

    public JavaSparkContext getContext() {
        return context;
    }

    @Override
    public Grid loadGrid(File path) throws IOException {
        return SparkGrid.loadFromFile(context, path);
    }

    @Override
    public Grid gridFromList(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        List<Tuple2<Long, Row>> tuples = IntStream.range(0, rows.size())
                .mapToObj(i -> new Tuple2<Long, Row>((long) i, rows.get(i)))
                .collect(Collectors.toList());
        JavaPairRDD<Long, Row> rdd = JavaPairRDD.fromJavaRDD(context.parallelize(tuples, defaultParallelism));
        return new SparkGrid(columnModel, rdd, overlayModels, this, rows.size(), -1);
    }

    static private Function0<BoxedUnit> sparkShutdownHook() {
        return new Function0<BoxedUnit>() {

            @Override
            public BoxedUnit apply() {
                if (ProjectManager.singleton != null) {
                    ProjectManager.singleton.dispose();
                    ProjectManager.singleton = null;
                }
                return BoxedUnit.UNIT;
            }

        };

    }

    public FileSystem getFileSystem() throws IOException {
        return FileSystem.get(context.hadoopConfiguration());
    }

    @Override
    public <T> ChangeData<T> loadChangeData(File path, ChangeDataSerializer<T> serializer)
            throws IOException {
        if (!path.exists()) {
            throw new IOException("Path " + path.toString() + " does not exist");
        }
        /*
         * The text files corresponding to each partition are read in the correct order thanks to our dedicated file
         * system OrderedLocalFileSystem. https://issues.apache.org/jira/browse/SPARK-5300
         */
        JavaPairRDD<Long, IndexedData<T>> data = context.textFile(path.getAbsolutePath())
                .map(line -> IndexedData.<T> read(line, serializer))
                .keyBy(IndexedData::getId)
                .persist(StorageLevel.MEMORY_ONLY());

        File completionMarker = new File(path, Runner.COMPLETION_MARKER_FILE_NAME);
        return new SparkChangeData<T>(data, this, completionMarker.exists());
    }

    protected static <T> Function<String, Tuple2<Long, T>> parseIndexedData(Type expectedType) {

        return new Function<>() {

            private static final long serialVersionUID = 3635263442656462809L;

            @Override
            public Tuple2<Long, T> call(String v1) throws Exception {
                TypeReference<IndexedData<T>> typeRef = new TypeReference<IndexedData<T>>() {

                    @Override
                    public Type getType() {
                        return expectedType;
                    }
                };
                IndexedData<T> id = ParsingUtilities.mapper.readValue(v1, typeRef);
                return new Tuple2<Long, T>(id.getId(), id.getData());
            }
        };
    }

    protected static <T> FlatMapFunction<Iterator<IndexedData<T>>, IndexedData<T>> completePartition() {
        return new FlatMapFunction<>() {

            @Override
            public Iterator<IndexedData<T>> call(Iterator<IndexedData<T>> indexedDataIterator) throws Exception {
                return IndexedData.completeIterator(indexedDataIterator);
            }
        };
    }

    @Override
    public <T> ChangeData<T> changeDataFromList(List<IndexedData<T>> changeData) {
        return changeDataFromList(changeData, true);
    }

    protected <T> ChangeData<T> changeDataFromList(List<IndexedData<T>> changeData, boolean isComplete) {
        List<Tuple2<Long, IndexedData<T>>> tuples = changeData.stream()
                .filter(id -> id.getData() != null)
                .map(i -> new Tuple2<>(i.getId(), i))
                .collect(Collectors.toList());
        JavaPairRDD<Long, IndexedData<T>> rdd = JavaPairRDD.fromJavaRDD(context.parallelize(tuples, defaultParallelism));
        return new SparkChangeData<T>(rdd, this, isComplete);
    }

    @Override
    public <T> ChangeData<T> emptyChangeData() {
        return changeDataFromList(Collections.emptyList(), false);
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException {
        return loadTextFile(path, progress, encoding, -1);
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException {
        // TODO find a way to pass the encoding here?
        JavaRDD<String> lines = context.textFile(path);
        ColumnModel columnModel = new ColumnModel(Collections.singletonList(new ColumnMetadata("Column")));
        JavaRDD<Row> rows = lines.map(s -> new Row(Collections.singletonList(new Cell(s, null))));
        if (limit >= 0) {
            // this generally leaves more rows than necessary, but is the best thing
            // we can do so far without reading the dataset to add row indices
            rows = RDDUtils.limitPartitions(rows, limit);
        }
        JavaPairRDD<Long, Row> indexedRows = RDDUtils.zipWithIndex(rows);
        if (limit >= 0) {
            // enforce limit properly by removing any rows from the following partitions
            // that exceed the desired row count
            indexedRows = RDDUtils.limit(indexedRows, limit);
        }
        return new SparkGrid(columnModel, indexedRows, Collections.emptyMap(), this);
    }

    @Override
    public boolean supportsProgressReporting() {
        // TODO add support for progress reporting
        return false;
    }

}
