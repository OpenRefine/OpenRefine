
package org.openrefine;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public class SparkBasedTest {

    protected SparkConf sparkConf = new SparkConf().setAppName("SparkBasedTest").setMaster("local");
    protected JavaSparkContext _context;

    @BeforeSuite
    public void setUpSpark() {
        _context = new JavaSparkContext(sparkConf);
    }

    protected JavaSparkContext context() {
        return _context;
    }

    @AfterSuite
    public void tearDownSpark() {
        _context.close();
    }
}
