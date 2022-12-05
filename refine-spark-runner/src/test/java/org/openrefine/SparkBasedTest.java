
package org.openrefine;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import scala.Tuple2;

import org.openrefine.io.OrderedLocalFileSystem;
import org.openrefine.model.Cell;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.util.RDDUtils;

public class SparkBasedTest {

    static final Logger logger = LoggerFactory.getLogger(SparkBasedTest.class);

    static {
        // set up Hadoop on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            try {
                System.setProperty("hadoop.home.dir", new File("../refine-spark-runner/hadoop").getCanonicalPath());
            } catch (IOException e) {
                logger.warn("unable to locate Windows Hadoop binaries, this will leave temporary files behind");
            }
        }
    }

    protected static SparkConf sparkConf = new SparkConf().setAppName("SparkBasedTest").setMaster("local");
    public static JavaSparkContext context = new JavaSparkContext(sparkConf);
    static {
        context.hadoopConfiguration().set("fs.file.impl", OrderedLocalFileSystem.class.getName());
    }

    protected JavaSparkContext context() {
        return context;
    }

    protected JavaPairRDD<Long, Row> rowRDD(Cell[][] cells, int numPartitions) {
        List<Tuple2<Long, Row>> rdd = new ArrayList<>(cells.length);
        for (int i = 0; i != cells.length; i++) {
            List<Cell> currentCells = new ArrayList<>(cells[i].length);
            for (int j = 0; j != cells[i].length; j++) {
                currentCells.add(cells[i][j]);
            }
            rdd.add(new Tuple2<Long, Row>((long) i, new Row(currentCells)));
        }
        return context().parallelize(rdd, numPartitions)
                .keyBy(t -> (Long) t._1)
                .mapValues(t -> t._2);
    }

    protected JavaPairRDD<Long, Row> rowRDD(Cell[][] cells) {
        return rowRDD(cells, 2);
    }

    protected JavaPairRDD<Long, Row> rowRDD(Serializable[][] cellValues, int numPartitions) {
        Cell[][] cells = new Cell[cellValues.length][];
        for (int i = 0; i != cellValues.length; i++) {
            cells[i] = new Cell[cellValues[i].length];
            for (int j = 0; j != cellValues[i].length; j++) {
                if (cellValues[i][j] != null) {
                    cells[i][j] = new Cell(cellValues[i][j], null);
                } else {
                    cells[i][j] = null;
                }
            }
        }
        return rowRDD(cells, numPartitions);
    }

    protected JavaPairRDD<Long, IndexedRow> indexedRowRDD(Serializable[][] cellValues, int numPartitions) {
        return RDDUtils.mapKeyValuesToValues(rowRDD(cellValues, numPartitions),
                (key, value) -> new IndexedRow(key, value));
    }

    protected JavaPairRDD<Long, Row> rowRDD(Serializable[][] cells) {
        return rowRDD(cells, 2);
    }

    @AfterSuite
    public void tearDownSpark() {
        context.close();
    }
}
