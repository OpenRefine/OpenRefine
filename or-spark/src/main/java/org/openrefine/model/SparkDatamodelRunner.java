
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.util.ShutdownHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function0;
import scala.Tuple2;
import scala.runtime.BoxedUnit;

import org.openrefine.ProjectManager;
import org.openrefine.io.OrderedLocalFileSystem;
import org.openrefine.overlay.OverlayModel;

/**
 * Spark implementation of the data model.
 * 
 * @author Antonin Delpeuch
 *
 */
public class SparkDatamodelRunner implements DatamodelRunner {

    static final Logger logger = LoggerFactory.getLogger(SparkDatamodelRunner.class);

    private JavaSparkContext context;
    private int defaultParallelism;

    public SparkDatamodelRunner(Integer defaultParallelism) {
        this.defaultParallelism = defaultParallelism;

        Thread.currentThread().setContextClassLoader(JavaSparkContext.class.getClassLoader());

        // set up Hadoop on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            try {
                System.setProperty("hadoop.home.dir", new File("server/target/lib/native/windows/hadoop").getCanonicalPath());
            } catch (IOException e) {
                logger.warn("unable to locate Windows Hadoop binaries, this will leave temporary files behind");
            }
        }

        context = new JavaSparkContext(
                new SparkConf()
                        .setAppName("OpenRefine")
                        .setMaster(String.format("local[%d]", defaultParallelism)));
        context.setLogLevel("WARN");
        context.hadoopConfiguration().set("fs.file.impl", OrderedLocalFileSystem.class.getName());

        // Set up hook to save projects when spark shuts down
        int priority = ShutdownHookManager.SPARK_CONTEXT_SHUTDOWN_PRIORITY() + 10;
        ShutdownHookManager.addShutdownHook(priority, sparkShutdownHook());
    }

    public SparkDatamodelRunner(JavaSparkContext context) {
        this.context = context;
        this.defaultParallelism = context.defaultParallelism();
    }

    public JavaSparkContext getContext() {
        return context;
    }

    @Override
    public GridState loadGridState(File path) throws IOException {
        return SparkGridState.loadFromFile(context, path);
    }

    @Override
    public GridState create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        List<Tuple2<Long, Row>> tuples = IntStream.range(0, rows.size())
                .mapToObj(i -> new Tuple2<Long, Row>((long) i, rows.get(i)))
                .collect(Collectors.toList());
        JavaPairRDD<Long, Row> rdd = JavaPairRDD.fromJavaRDD(context.parallelize(tuples, defaultParallelism));
        return new SparkGridState(columnModel, rdd, overlayModels);
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

    @Override
    public FileSystem getFileSystem() throws IOException {
        return FileSystem.get(context.hadoopConfiguration());
    }

}
