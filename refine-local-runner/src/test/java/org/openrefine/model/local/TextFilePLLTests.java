
package org.openrefine.model.local;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    File longerTextFile;
    File veryLongTextFile;
    Charset utf8 = Charset.forName("UTF-8");

    @BeforeTest
    public void setUp() throws IOException {
        tempDir = TestUtils.createTempDirectory("datamodelrunnertest");

        textFile = new File(tempDir, "textfile.txt");
        createTestTextFile(textFile, "foo\nbar\nbaz");

        // this file will have 9 * 64 - 1 = 575 characters, which is enough to be split in 4 partitions of more than 128
        // bytes
        longerTextFile = new File(tempDir, "longertextfile.txt");
        createTestTextFile(longerTextFile, String.join("\n", Collections.nCopies(64, "aaaaaaaa")));

        // this file will have 9 * 2048 - 1 = 18431 characters, which is too much to be split in 4 partitions only
        veryLongTextFile = new File(tempDir, "verylongtextfile.txt");
        createTestTextFile(veryLongTextFile, String.join("\n", Collections.nCopies(2048, "aaaaaaaa")));
    }

    @AfterTest
    public void tearDown() {
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            e.printStackTrace();
            tempDir = null;
        }
    }

    @Test
    public void testLoadTextFile() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        // this text file is too small to be split
        Assert.assertEquals(pll.getPartitions().size(), 1);

        List<String> elements = pll.collect();
        Assert.assertEquals(elements, Arrays.asList("foo", "bar", "baz"));
        // Iterate a second time
        Assert.assertEquals(elements, Arrays.asList("foo", "bar", "baz"));
    }

    @Test
    public void testMorePartitions() throws IOException {
        PLL<String> pll = new TextFilePLL(context, longerTextFile.getAbsolutePath(), utf8);
        Assert.assertEquals(pll.getPartitions().size(), context.getDefaultParallelism());
        Assert.assertEquals(pll.count(), 64L);

        pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();
        Assert.assertTrue(nbPartitions > context.getDefaultParallelism());
        Assert.assertEquals(pll.count(), 2048L);
    }

    @Test
    public void testRoundTripSerialization() throws IOException, InterruptedException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        File tempFile = new File(tempDir, "roundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), Optional.empty());

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8);

        Assert.assertEquals(pll.collect(), deserializedPLL.collect());
    }

    @Test
    public void testLargerRoundTripSerialization() throws IOException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();

        File tempFile = new File(tempDir, "largerroundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), Optional.empty());

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8);
        Assert.assertEquals(deserializedPLL.getPartitions().size(), nbPartitions);
        Assert.assertEquals(deserializedPLL.count(), 2048L);
    }

    @Test
    public void testSaveWithoutCachedPartitionSizes() throws IOException, InterruptedException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        // artificially discard partition sizes
        pll.cachedPartitionSizes = null;

        File tempFile = new File(tempDir, "progress-no-partition-sizes.txt");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        pll.saveAsTextFile(tempFile.getAbsolutePath(), Optional.of(progressReporter));
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testSaveWithCachedPartitionSizes() throws IOException, InterruptedException {
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
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        // partition sizes are not known
        Assert.assertNull(pll.cachedPartitionSizes);

        ProgressReporterStub progressReporter = new ProgressReporterStub();
        pll.cache(Optional.of(progressReporter));
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testCacheWithProgressReportingAndPrecomputedPartitionSizes() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
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
