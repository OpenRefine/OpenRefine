
package org.openrefine.runners.local.pll;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.process.ProgressReporterStub;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.CloseableIterable;
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

        // this file will have 9 * 64 - 1 = 575 characters, which is enough to be split in 4 partitions of more than 128
        // bytes
        longerTextFile = new File(tempDir, "longertextfile.txt");
        createTestTextFile(longerTextFile, String.join("\n", Collections.nCopies(64, "aaaaaaaa")));

        // this file will have 9 * 2048 - 1 = 18431 characters, which is too much to be split in 4 partitions only
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

        List<String> elements = pll.collect().toJavaList();
        Assert.assertEquals(elements, Arrays.asList("foo", "bar", "baz"));
        // Iterate a second time
        Assert.assertEquals(elements, Arrays.asList("foo", "bar", "baz"));
    }

    @Test
    public void testToString() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        // the string representation of the PLL is a sort of query plan - not its contents
        Assert.assertTrue(pll.toString().contains("Text file"));
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
        pll.saveAsTextFile(tempFile.getAbsolutePath(), 0, false, false);

        // check for presence of the _SUCCESS marker
        File successMarker = new File(tempFile, "_SUCCESS");
        Assert.assertTrue(successMarker.exists());

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8);

        Assert.assertEquals(pll.collect(), deserializedPLL.collect());
    }

    @Test
    public void testLargerRoundTripSerialization() throws IOException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();

        File tempFile = new File(tempDir, "largerroundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), 0, false, false);

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8);
        Assert.assertEquals(deserializedPLL.getPartitions().size(), nbPartitions);
        Assert.assertEquals(deserializedPLL.count(), 2048L);
    }

    @Test
    public void testSaveWithoutCachedPartitionSizes() throws InterruptedException, ExecutionException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        // artificially discard partition sizes
        pll = pll.filter(x -> true);

        File tempFile = new File(tempDir, "progress-no-partition-sizes.txt");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = pll.saveAsTextFileAsync(tempFile.getAbsolutePath(), 0, false, false);
        future.onProgress(progressReporter);
        future.get();
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testSaveWithCachedPartitionSizes() throws InterruptedException, ExecutionException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        // the sizes of the partitions are known
        Assert.assertTrue(pll.hasCachedPartitionSizes());

        File tempFile = new File(tempDir, "progress-with-partition-sizes.txt");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = pll.saveAsTextFileAsync(tempFile.getAbsolutePath(), 0, false, false);
        future.onProgress(progressReporter);
        future.get();
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testSaveAndRepartition() throws InterruptedException, ExecutionException {
        List<String> strings = Arrays.asList(
                "foo", "bar", "baz", "boo",
                "hop", "fip", "bal", "rum",
                "cup", "cap", "dip", "big",
                "fit", "tan", "hat", "nop");
        PLL<String> pll = new SinglePartitionPLL<>(context, CloseableIterable.of(strings), 16L);
        // the sizes of the partitions are known: there is a single one
        Assert.assertTrue(pll.hasCachedPartitionSizes());
        Assert.assertEquals(pll.getPartitions().size(), 1);

        File tempFile = new File(tempDir, "save-and-repartition");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = pll.saveAsTextFileAsync(tempFile.getAbsolutePath(), 0, true, false);
        future.onProgress(progressReporter);
        future.get();
        Assert.assertEquals(progressReporter.getPercentage(), 100);
        Assert.assertTrue(new File(tempFile, "part-00000.gz").exists());
        Assert.assertTrue(new File(tempFile, "part-00001.gz").exists());
        Assert.assertTrue(new File(tempFile, "part-00002.gz").exists());
        Assert.assertTrue(new File(tempFile, "part-00003.gz").exists());
    }

    @Test
    public void testCacheWithProgressReporting() throws IOException, ExecutionException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        // partition sizes are not known
        Assert.assertFalse(pll.hasCachedPartitionSizes());

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = pll.cacheAsync();
        future.onProgress(progressReporter);
        future.get();
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testCacheWithProgressReportingAndPrecomputedPartitionSizes() throws IOException, ExecutionException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        pll.count();
        // partition sizes are known
        Assert.assertTrue(pll.hasCachedPartitionSizes());

        ProgressReporterStub progressReporter = new ProgressReporterStub();
        ProgressingFuture<Void> future = pll.cacheAsync();
        future.onProgress(progressReporter);
        future.get();
        Assert.assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testReadIncompletePLL() throws IOException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();

        File tempFile = new File(tempDir, "largerroundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), 0, false, false);

        // truncate various partitions at various sizes and remove the completion marker
        truncateFile(new File(tempFile, "part-00001.gz"), 1);
        truncateFile(new File(tempFile, "part-00002.gz"), 2);
        truncateFile(new File(tempFile, "part-00003.gz"), 3);
        truncateFile(new File(tempFile, "part-00004.gz"), 4);
        truncateFile(new File(tempFile, "part-00005.gz"), 5);
        truncateFile(new File(tempFile, "part-00006.gz"), 6);
        truncateFile(new File(tempFile, "part-00007.gz"), 10);
        truncateFile(new File(tempFile, "part-00008.gz"), 20);
        File successMarker = new File(tempFile, "_SUCCESS");
        successMarker.delete();

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8, true);
        Assert.assertEquals(deserializedPLL.getPartitions().size(), nbPartitions);
        Assert.assertEquals(deserializedPLL.count(), 1137L);
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

    // Helper to simulate a truncated file obtained from an interrupted serialization
    protected void truncateFile(File file, int maxLength) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] contents = fis.readNBytes(maxLength);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(contents);
            }
        }
    }
}
