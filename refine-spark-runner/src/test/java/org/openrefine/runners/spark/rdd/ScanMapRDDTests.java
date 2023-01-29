
package org.openrefine.runners.spark.rdd;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;
import scala.reflect.ClassManifestFactory;
import scala.reflect.ClassTag;

import org.openrefine.runners.spark.SparkBasedTest;

public class ScanMapRDDTests extends SparkBasedTest {

    private static final ClassTag<Long> longTag = ClassManifestFactory.fromClass(Long.class);
    private static final ClassTag<String> stringTag = ClassManifestFactory.fromClass(String.class);
    @SuppressWarnings("unchecked")
    private static final ClassTag<Tuple2<Long, String>> tupleTag = ClassManifestFactory
            .fromClass((Class<Tuple2<Long, String>>) (Class<?>) Tuple2.class);

    private static Function<String, Long> zipWithIndexFeed() {
        return new Function<String, Long>() {

            private static final long serialVersionUID = 8491496353557493796L;

            @Override
            public Long call(String v1) {
                return 1L;
            }

        };
    }

    private static Function2<Long, Long, Long> sumLong() {
        return new Function2<Long, Long, Long>() {

            private static final long serialVersionUID = 8821647027206656050L;

            @Override
            public Long call(Long v1, Long v2) {
                return v1 + v2;
            }

        };
    }

    private static Function2<Long, String, Tuple2<Long, String>> makeTuple() {
        return new Function2<Long, String, Tuple2<Long, String>>() {

            private static final long serialVersionUID = 3577445993352394527L;

            @Override
            public Tuple2<Long, String> call(Long v1, String v2) {
                return new Tuple2<Long, String>(v1, v2);
            }

        };
    }

    @Test
    public void testZipWithIndexViaScanMap() {
        List<String> list = Arrays.asList("lorem", "ipsum", "dolor", "sit", "amet,", "consectetur", "adipiscing", "elit");
        // Replicates zipWithIndex with scanMap
        JavaRDD<String> origRDD = context.parallelize(list, 3);

        ScanMapRDD<Long, String, Tuple2<Long, String>> zipped = new ScanMapRDD<Long, String, Tuple2<Long, String>>(
                origRDD.rdd(),
                zipWithIndexFeed(), sumLong(), makeTuple(), 0L,
                tupleTag, stringTag, longTag);

        List<Tuple2<Long, String>> zippedResults = new JavaPairRDD<Long, String>(zipped, longTag, stringTag).collect();
        Assert.assertEquals(zippedResults,
                Arrays.asList(
                        new Tuple2<Long, String>(0L, "lorem"),
                        new Tuple2<Long, String>(1L, "ipsum"),
                        new Tuple2<Long, String>(2L, "dolor"),
                        new Tuple2<Long, String>(3L, "sit"),
                        new Tuple2<Long, String>(4L, "amet,"),
                        new Tuple2<Long, String>(5L, "consectetur"),
                        new Tuple2<Long, String>(6L, "adipiscing"),
                        new Tuple2<Long, String>(7L, "elit")));
    }

}
