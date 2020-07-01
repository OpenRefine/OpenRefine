
package org.openrefine.model.rdd;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;

import org.openrefine.SparkBasedTest;

public class ZippedWithIndexRDDTests extends SparkBasedTest {

    @Test
    public void testZipWithIndex() {
        JavaRDD<String> initial = context.parallelize(Arrays.asList("foo", "bar", "hello", "world", "today"), 3);

        JavaPairRDD<Long, String> zipped = new ZippedWithIndexRDD<String>(initial).asPairRDD();

        List<Tuple2<Long, String>> result = zipped.collect();

        Assert.assertEquals(result, Arrays.asList(
                new Tuple2<Long, String>(0L, "foo"),
                new Tuple2<Long, String>(1L, "bar"),
                new Tuple2<Long, String>(2L, "hello"),
                new Tuple2<Long, String>(3L, "world"),
                new Tuple2<Long, String>(4L, "today")));
    }
}
