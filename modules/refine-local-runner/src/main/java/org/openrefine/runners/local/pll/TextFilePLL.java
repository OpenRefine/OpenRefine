
package org.openrefine.runners.local.pll;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import com.google.common.io.CountingInputStream;
import io.vavr.collection.Array;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.Runner;
import org.openrefine.runners.local.pll.util.LineReader;
import org.openrefine.util.CloseableIterator;

/**
 * A PLL whose contents are read from a set of text files. The text files are partitioned using a method similar to that
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
    private final boolean ignoreEarlyEOF;
    private ReadingProgressReporter progress;

    public TextFilePLL(PLLContext context, String path, Charset encoding) throws IOException {
        this(context, path, encoding, false);
    }

    /**
     * Constructs a PLL out of a text file.
     *
     * @param context
     *            the associated context, whose thread pool will be used
     * @param path
     *            the path to the file or directory whose contents should be read
     * @param encoding
     *            the encoding in which the files should be read
     * @param ignoreEarlyEOF
     *            whether to ignore early ends of files, due to an interrupted write
     */
    public TextFilePLL(PLLContext context, String path, Charset encoding, boolean ignoreEarlyEOF) throws IOException {
        super(context, "Text file from " + path);
        this.path = path;
        this.encoding = encoding;
        this.ignoreEarlyEOF = ignoreEarlyEOF;
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
                // the completion marker should not be read as a partition, because it is empty
                if (!Runner.COMPLETION_MARKER_FILE_NAME.equals(subFile.getName())) {
                    addPartitionsForFile(subFile);
                }
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
    protected CloseableIterator<String> compute(Partition partition) {
        TextFilePartition textPartition = (TextFilePartition) partition;

        int reportBatchSize = 64;
        try {
            FileInputStream stream = new FileInputStream(textPartition.getPath());
            FileChannel channel = stream.getChannel();
            if (textPartition.getStart() > 0L) {
                channel.position(textPartition.start);
            }
            CountingInputStream countingIs;

            LineReader lineReader; // used when we need to keep track of exact consumption of bytes from the source
                                   // stream
            LineNumberReader lineNumberReader; // used when we do not need to keep track (faster)

            if (isGzipped(textPartition.getPath())) {
                // if we decompress, we count the bytes before decompression (since that is how the file size was
                // computed).
                InputStream bufferedIs = new BufferedInputStream(stream);
                countingIs = new CountingInputStream(bufferedIs);
                try {
                    bufferedIs = new GZIPInputStream(countingIs);
                } catch (IOException e) {
                    if (ignoreEarlyEOF) {
                        // this partition was truncated very early, in the Gzip header.
                        // No content in it is recoverable. We replace it by an empty stream
                        bufferedIs = new ByteArrayInputStream(new byte[] {});
                    } else {
                        throw e;
                    }
                }
                lineReader = null;
                lineNumberReader = new LineNumberReader(new InputStreamReader(bufferedIs, encoding));
            } else {
                countingIs = new CountingInputStream(new BufferedInputStream(stream));
                lineReader = new LineReader(countingIs, encoding);
                lineNumberReader = null;
            }

            // if we are not reading from the first partition of the given file,
            // we need to ignore the first "line" because it might be incomplete
            // (it might have started before the split).
            // The reading of the previous partition will take care of reading that line fully
            // (so it can go beyond the planned end of the split).
            if (textPartition.getStart() > 0) {
                lineReader.readLine();
            }

            CloseableIterator<String> iterator = new CloseableIterator<>() {

                boolean nextLineAttempted = false;
                String nextLine = null;
                long lastOffsetReported = -1;
                long lastOffsetSeen = -1;
                int lastReport = 0;
                boolean closed = false;

                @Override
                public boolean hasNext() {
                    long currentPosition;
                    try {
                        currentPosition = textPartition.start + (countingIs == null ? 0 : countingIs.getCount());
                        if (!nextLineAttempted && currentPosition <= textPartition.getEnd()) {
                            if (lineNumberReader != null) {
                                nextLine = lineNumberReader.readLine();
                            } else {
                                nextLine = lineReader.readLine();
                            }
                            nextLineAttempted = true;
                            lastOffsetSeen = currentPosition;
                        }
                        if (nextLine == null && lastOffsetSeen > lastOffsetReported) {
                            reportProgress();
                        }
                    } catch (EOFException | ZipException e) {
                        if (ignoreEarlyEOF) {
                            nextLine = null;
                        } else {
                            throw new UncheckedIOException(e);
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

                @Override
                public void close() {
                    if (!closed) {
                        try {
                            if (lineReader != null) {
                                lineReader.close();
                            } else {
                                lineNumberReader.close();
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        } finally {
                            closed = true;
                        }
                    }
                }
            };
            return iterator;
        } catch (IOException e) {
            e.printStackTrace();
            return CloseableIterator.empty();
        }
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return Array.ofAll(partitions);
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
