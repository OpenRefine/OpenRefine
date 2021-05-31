
package org.openrefine.model.local;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.process.ProgressReporterStub;
import org.openrefine.util.TestUtils;

public class TextFilePLLTests extends PLLTestsBase {

    File tempDir;
    File textFile;

    @BeforeTest
    public void setUp() throws IOException {
        tempDir = TestUtils.createTempDirectory("datamodelrunnertest");
        textFile = new File(tempDir, "textfile.txt");
        createTestTextFile(textFile, "foo\nbar\nbaz");
    }

    @AfterTest
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
        tempDir = null;
    }

    @Test
    public void testLoadTextFile() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath());

        Assert.assertEquals(pll.collect(), Arrays.asList("foo", "bar", "baz"));
        // Iterate a second time
        Assert.assertEquals(pll.collect(), Arrays.asList("foo", "bar", "baz"));
    }

    @Test
    public void testRoundTripSerialization() throws IOException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        File tempFile = new File(tempDir, "roundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), Optional.empty());

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath());

        Assert.assertEquals(pll.collect(), deserializedPLL.collect());
    }

    @Test
    public void testSaveWithoutCachedPartitionSizes() throws IOException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        // artificially discard partition sizes
        pll.cachedPartitionSizes = null;

        File tempFile = new File(tempDir, "progress-no-partition-sizes.txt");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        pll.saveAsTextFile(tempFile.getAbsolutePath(), Optional.of(progressReporter));
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testSaveWithCachedPartitionSizes() throws IOException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        // the sizes of the partitions are known
        Assert.assertNotNull(pll.cachedPartitionSizes);

        File tempFile = new File(tempDir, "progress-with-partition-sizes.txt");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        pll.saveAsTextFile(tempFile.getAbsolutePath(), Optional.of(progressReporter));
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testCacheWithProgressReporting() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath());
        // partition sizes are not known
        Assert.assertNull(pll.cachedPartitionSizes);

        ProgressReporterStub progressReporter = new ProgressReporterStub();
        pll.cache(Optional.of(progressReporter));
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testCacheWithProgressReportingAndPrecomputedPartitionSizes() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath());
        pll.count();
        // partition sizes are known
        Assert.assertNotNull(pll.cachedPartitionSizes);

        ProgressReporterStub progressReporter = new ProgressReporterStub();
        pll.cache(Optional.of(progressReporter));
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    protected void createTestTextFile(File file, String contents) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(contents);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }
}
