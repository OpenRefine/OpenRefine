
package org.openrefine.runners.local.pll;

import static java.lang.Thread.sleep;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.vavr.collection.Array;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.process.ProgressReporterStub;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.IOUtils;
import org.openrefine.util.TestUtils;

public class TextFilePLLTests extends PLLTestsBase {

    File tempDir;
    File textFile;
    File longerTextFile;
    File veryLongTextFile;
    File incompleteChangeData;
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

        // file where the Gzip decompressor fails early but we should still be able to read a few lines
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("incomplete-partition.gz")) {
            File subDir = new File(tempDir, "incomplete");
            subDir.mkdir();
            incompleteChangeData = subDir;
            File partitionFile = new File(subDir, "part-00000.gz");
            IOUtils.copy(stream, partitionFile);
        }
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
        assertEquals(pll.getPartitions().size(), 1);

        List<String> elements = pll.collect().toJavaList();
        assertEquals(elements, Arrays.asList("foo", "bar", "baz"));
        // Iterate a second time
        assertEquals(elements, Arrays.asList("foo", "bar", "baz"));
    }

    @Test
    public void testToString() throws IOException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        // the string representation of the PLL is a sort of query plan - not its contents
        assertTrue(pll.toString().contains("Text file"));
    }

    @Test
    public void testMorePartitions() throws IOException {
        PLL<String> pll = new TextFilePLL(context, longerTextFile.getAbsolutePath(), utf8);
        assertEquals(pll.getPartitions().size(), context.getDefaultParallelism());
        assertEquals(pll.count(), 64L);

        pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();
        assertTrue(nbPartitions > context.getDefaultParallelism());
        assertEquals(pll.count(), 2048L);
    }

    @Test
    public void testRoundTripSerialization() throws IOException, InterruptedException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        File tempFile = new File(tempDir, "roundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), 0, false, false);

        // check for presence of the _SUCCESS marker
        File successMarker = new File(tempFile, "_SUCCESS");
        assertTrue(successMarker.exists());

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8);

        assertEquals(pll.collect(), deserializedPLL.collect());
    }

    @Test
    public void testLargerRoundTripSerialization() throws IOException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();

        File tempFile = new File(tempDir, "largerroundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), 0, false, false);

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8);
        assertEquals(deserializedPLL.getPartitions().size(), nbPartitions);
        assertEquals(deserializedPLL.count(), 2048L);
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
        assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testSaveWithCachedPartitionSizes() throws InterruptedException, ExecutionException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        // the sizes of the partitions are known
        assertTrue(pll.hasCachedPartitionSizes());

        File tempFile = new File(tempDir, "progress-with-partition-sizes.txt");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = pll.saveAsTextFileAsync(tempFile.getAbsolutePath(), 0, false, false);
        future.onProgress(progressReporter);
        future.get();
        assertEquals(progressReporter.getPercentage(), 100);
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
        assertTrue(pll.hasCachedPartitionSizes());
        assertEquals(pll.getPartitions().size(), 1);

        File tempFile = new File(tempDir, "save-and-repartition");

        ProgressReporterStub progressReporter = new ProgressReporterStub();

        ProgressingFuture<Void> future = pll.saveAsTextFileAsync(tempFile.getAbsolutePath(), 0, true, false);
        future.onProgress(progressReporter);
        future.get();
        assertEquals(progressReporter.getPercentage(), 100);
        assertTrue(new File(tempFile, "part-00000.zst").exists());
        assertTrue(new File(tempFile, "part-00001.zst").exists());
        assertTrue(new File(tempFile, "part-00002.zst").exists());
        assertTrue(new File(tempFile, "part-00003.zst").exists());
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
        assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testCacheWithProgressReportingAndPrecomputedPartitionSizes() throws IOException, ExecutionException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, textFile.getAbsolutePath(), utf8);
        pll.count();
        // partition sizes are known
        assertTrue(pll.hasCachedPartitionSizes());

        ProgressReporterStub progressReporter = new ProgressReporterStub();
        ProgressingFuture<Void> future = pll.cacheAsync();
        future.onProgress(progressReporter);
        future.get();
        assertEquals(progressReporter.getPercentage(), 100);
    }

    @Test
    public void testReadIncompleteChangeData() throws IOException {
        // this is a change data partition observed in the wild, which used not to be read at all
        // (with the decompressor failing before any line could be read)
        PLL<String> pll = new TextFilePLL(context, incompleteChangeData.getAbsolutePath(), utf8, true, "end");
        Array<String> lines = pll.collect();
        assertTrue(lines.size() >= 20);
    }

    @Test
    public void testReadIncompletePLL() throws IOException, InterruptedException {
        PLL<String> pll = new TextFilePLL(context, veryLongTextFile.getAbsolutePath(), utf8);
        int nbPartitions = pll.getPartitions().size();

        File tempFile = new File(tempDir, "largerroundtrip.txt");
        pll.saveAsTextFile(tempFile.getAbsolutePath(), 0, false, false);

        // truncate various partitions at various sizes and remove the completion marker
        truncateFile(new File(tempFile, "part-00001.zst"), 1);
        truncateFile(new File(tempFile, "part-00002.zst"), 2);
        truncateFile(new File(tempFile, "part-00003.zst"), 3);
        truncateFile(new File(tempFile, "part-00004.zst"), 4);
        truncateFile(new File(tempFile, "part-00005.zst"), 5);
        truncateFile(new File(tempFile, "part-00006.zst"), 6);
        truncateFile(new File(tempFile, "part-00007.zst"), 10);
        truncateFile(new File(tempFile, "part-00008.zst"), 20);
        File successMarker = new File(tempFile, "_SUCCESS");
        successMarker.delete();

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8, true, "end");
        assertEquals(deserializedPLL.getPartitions().size(), nbPartitions);
        assertTrue(deserializedPLL.count() > 1000L); // it is 1196 as of 2023-06-02 with java 11
        // but I suspect the exact count can be JVM-dependent
    }

    @Test
    public void testSynchronousRead() throws IOException, InterruptedException {
        File tempFile = new File(tempDir, "synchronous_read");
        tempFile.mkdir();
        try (FileOutputStream fos = new FileOutputStream(new File(tempFile, "part-0000.zst"));
                ZstdCompressorOutputStream gos = new ZstdCompressorOutputStream(fos);
                Writer writer = new OutputStreamWriter(gos, StandardCharsets.UTF_8)) {

            // write a few lines before we start reading anything
            writer.write("hello\n");
            writer.write("to\n");
            writer.write("the\n");
            writer.write("world\n");
            writer.flush();

            TextFilePLL pll = new TextFilePLL(context, tempFile.getAbsolutePath(), utf8, true, "end");

            List<String> readStrings = new ArrayList<>();

            Thread consumer = new Thread(() -> {
                try (CloseableIterator<String> iterator = pll.blockingIterator()) {
                    for (String line : iterator) {
                        synchronized (readStrings) {
                            readStrings.add(line);
                        }
                    }
                }
            }, "StreamReader");
            consumer.start();

            // give the reader a bit of time to read some lines
            sleep(200);
            // we should have read one line at least
            synchronized (readStrings) {
                assertTrue(readStrings.size() > 0 && readStrings.size() <= 4);
                assertEquals(readStrings.get(0), "hello");
            }

            writer.write("and\n");
            writer.write("the\n");
            writer.write("rest\n");
            writer.write("of\n");
            writer.write("the\n");
            writer.write("universe\n");
            writer.write("end\n");
            writer.flush();
            writer.close();

            // give the reader a bit of time to catch up
            sleep(200);
            synchronized (readStrings) {
                // we should have read all the lines (without the end marker)
                assertEquals(readStrings.size(), 10);
                assertEquals(readStrings.get(9), "universe");
            }

            // start writing a second partition
            try (FileOutputStream fos2 = new FileOutputStream(new File(tempFile, "part-0001.zst"));
                    ZstdCompressorOutputStream gos2 = new ZstdCompressorOutputStream(fos2);
                    Writer writer2 = new OutputStreamWriter(gos2, StandardCharsets.UTF_8)) {

                // write a few lines in the second partition
                writer2.write("hi\n");
                writer2.write("from\n");
                writer2.write("the\n");
                writer2.write("second\n");
                writer2.write("partition\n");
                writer2.flush();

                // give the reader a bit of time to catch up
                sleep(200);
                synchronized (readStrings) {
                    // we should have read all the lines (without the end marker)
                    assertTrue(readStrings.size() >= 11);
                    assertEquals(readStrings.get(10), "hi");
                }
            }
        }
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
