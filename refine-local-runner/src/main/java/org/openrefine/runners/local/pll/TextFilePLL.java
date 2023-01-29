
package org.openrefine.runners.local.pll;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import com.google.common.io.CountingInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.runners.local.pll.util.LineReader;

/**
 * A PLL whose contents are read from a set of text files. The text files are partitioned using a method similar to that
 * of Hadoop, using new lines as boundaries.
 * 
 * This class aims at producing a certain number of partitions determined by the default parallelism of the PLL context.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TextFilePLL extends PLL<String> {

    private final static Logger logger = LoggerFactory.getLogger(TextFilePLL.class);
    private final List<TextFilePartition> partitions;
    private final String path;
    private final Charset encoding;
    private ReadingProgressReporter progress;

    public TextFilePLL(PLLContext context, String path, Charset encoding) throws IOException {
        super(context, "Text file from " + path);
        this.path = path;
        this.encoding = encoding;
        this.progress = null;

        File file = new File(path);
        partitions = new ArrayList<>();
        if (file.isDirectory()) {
            List<File> files = Arrays.asList(file.listFiles());
            files.sort(new Comparator<File>() {

                @Override
                public int compare(File arg0, File arg1) {
                    return arg0.getPath().compareTo(arg1.getPath());
                }
            });

            for (File subFile : files) {
                addPartitionsForFile(subFile);
            }
        } else {
            addPartitionsForFile(file);
        }
    }

    private static boolean isGzipped(File file) {
        return file.getName().endsWith(".gz");
    }

    private void addPartitionsForFile(File file) throws IOException {
        long size = Files.size(file.toPath());
        if (size < context.getMinSplitSize() * context.getDefaultParallelism() || isGzipped(file)) {
            // a single split
            partitions.add(new TextFilePartition(file, partitions.size(), 0L, size));
        } else {
            // defaultParallelism many splits, unless that makes splits too big
            long splitSize = Math.min((size / context.getDefaultParallelism()) + 1, context.getMaxSplitSize());
            int numSplits = (int) (size / splitSize);
            if (numSplits * splitSize < size) {
                numSplits++;
            }
            for (int i = 0; i != numSplits; i++) {
                partitions.add(new TextFilePartition(file, partitions.size(), splitSize * i, Math.min(splitSize * (i + 1), size)));
            }
        }
    }

    public void setProgressHandler(MultiFileReadingProgress progress) {
        // Try to extract the filename of the supplied path, fallback on the full path otherwise
        String filename = path;
        try {
            File file = new File(path);
            filename = file.getName();
        } catch (Exception e) {
            ;
        }
        this.progress = progress == null ? null : new ReadingProgressReporter(progress, filename);
    }

    @Override
    protected Stream<String> compute(Partition partition) {
        TextFilePartition textPartition = (TextFilePartition) partition;

        int reportBatchSize = 64;
        try {
            FileInputStream stream = new FileInputStream(textPartition.getPath());
            FileChannel channel = stream.getChannel();
            if (textPartition.getStart() > 0L) {
                channel.position(textPartition.start);
            }
            InputStream bufferedIs = new BufferedInputStream(stream);
            CountingInputStream countingIs;
            LineReader lineReader;
            if (isGzipped(textPartition.getPath())) {
                // if we decompress, we count the bytes before decompression (since that is how the file size was
                // computed).
                countingIs = new CountingInputStream(bufferedIs);
                bufferedIs = new GzipCompressorInputStream(bufferedIs);
                lineReader = new LineReader(bufferedIs, encoding);

            } else {
                countingIs = new CountingInputStream(bufferedIs);
                lineReader = new LineReader(countingIs, encoding);
            }

            // if we are not reading from the first partition of the given file,
            // we need to ignore the first "line" because it might be incomplete
            // (it might have started before the split).
            // The reading of the previous partition will take care of reading that line fully
            // (so it can go beyond the planned end of the split).
            if (textPartition.getStart() > 0) {
                lineReader.readLine();
            }

            Iterator<String> iterator = new Iterator<String>() {

                boolean nextLineAttempted = false;
                String nextLine = null;
                long lastOffsetReported = -1;
                long lastOffsetSeen = -1;
                int lastReport = 0;

                @Override
                public boolean hasNext() {
                    long currentPosition;
                    try {
                        currentPosition = textPartition.start + countingIs.getCount();
                        if (!nextLineAttempted && currentPosition <= textPartition.getEnd()) {
                            nextLine = lineReader.readLine();
                            nextLineAttempted = true;
                            lastOffsetSeen = currentPosition;
                        }
                        if (nextLine == null && lastOffsetSeen > lastOffsetReported) {
                            reportProgress();
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return nextLine != null;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("End of stream");
                    }
                    if (lastReport >= reportBatchSize) {
                        reportProgress();
                    }
                    lastReport++;
                    if (lastOffsetReported == -1) {
                        lastOffsetReported = lastOffsetSeen;
                    }
                    String line = nextLine;
                    nextLine = null;
                    nextLineAttempted = false;
                    return line;
                }

                private void reportProgress() {
                    if (progress != null) {
                        progress.increment(lastOffsetSeen - lastOffsetReported);
                        lastReport = 0;
                        lastOffsetReported = lastOffsetSeen;
                    }
                }

            };
            Stream<String> lineStream = Streams.stream(iterator)
                    .onClose(() -> {
                        try {
                            lineReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            return lineStream;
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    @Override
    public List<? extends Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.emptyList();
    }

    protected static class TextFilePartition implements Partition {

        private final File path;
        private final int index;
        private final long start;
        private final long end;

        /**
         * Represents a split in an uncompressed text file.
         * 
         * @param path
         *            the path to the file being read
         * @param index
         *            position of the split in the file
         * @param start
         *            starting byte where to read from in the file
         * @param end
         *            first byte not to be read after the end of the file
         */
        protected TextFilePartition(File path, int index, long start, long end) {
            this.path = path;
            this.index = index;
            this.start = start;
            this.end = end;
            if (isGzipped(path) && start > 0) {
                throw new IllegalArgumentException("Unable to split a gzip file");
            }
        }

        public File getPath() {
            return path;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Partition getParent() {
            return null;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

    }

}
