
package org.openrefine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import scala.Tuple2;

import org.openrefine.io.OrderedLocalFileSystem;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;

public class SparkBasedTest {

    protected static SparkConf sparkConf = new SparkConf().setAppName("SparkBasedTest").setMaster("local");
    protected static JavaSparkContext _context;

    @BeforeSuite
    public void setUpSpark() {
        _context = new JavaSparkContext(sparkConf);
        _context.hadoopConfiguration().set("fs.file.impl", OrderedLocalFileSystem.class.getName());
    }

    protected JavaSparkContext context() {
        return _context;
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

    protected JavaPairRDD<Long, Row> rowRDD(Serializable[][] cellValues) {
        Cell[][] cells = new Cell[cellValues.length][];
        for (int i = 0; i != cellValues.length; i++) {
            cells[i] = new Cell[cellValues[i].length];
            for (int j = 0; j != cellValues[i].length; j++) {
                cells[i][j] = new Cell(cellValues[i][j], null);
            }
        }
        return rowRDD(cells, 2);
    }

    @AfterSuite
    public void tearDownSpark() {
        _context.close();
    }
}
