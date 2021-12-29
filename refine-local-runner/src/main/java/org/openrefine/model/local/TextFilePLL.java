package org.openrefine.model.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.openrefine.importers.MultiFileReadingProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;

/**
 * A PLL whose contents are read from a set of text files.
 * The text files are partitioned using Hadoop, using new lines as boundaries.
 * 
 * This class aims at producing a certain number of partitions determined by the
 * default parallelism of the PLL context.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TextFilePLL extends PLL<String> {
    
    private final static Logger logger = LoggerFactory.getLogger(TextFilePLL.class);
    private final List<TextFilePartition> partitions;
    private final String path;
    private ReadingProgressReporter progress;

    public TextFilePLL(PLLContext context, String path) throws IOException {
        super(context);
        this.path = path;
        this.progress = null;
        
        Path file = Paths.get(path);
        long size = Files.size(file);
        
        partitions = new ArrayList<>();
        if (size < context.getMinSplitSize() * context.getDefaultParallelism()) {
            // a single split
            partitions.add(new TextFilePartition(0, 0L, size));
        } else {
            // defaultParallelism many splits, unless that makes splits too big
            long splitSize = Math.min(size / context.getDefaultParallelism(), context.getMaxSplitSize());
            int numSplits = (int) (size / splitSize);
            for (int i = 0; i != numSplits; i++) {
                partitions.add(new TextFilePartition(i, splitSize*i, Math.min(splitSize*(i+1), size)));
            }
        }    
    }
    
    public void setProgressHandler(MultiFileReadingProgress progress) {
        // Try to extract the filename of the supplied path, fallback on the full path otherwise
        String filename = path;
        try {
            File file = new File(path);
            filename = file.getName();
        } catch(Exception e) {
            ;
        }
        this.progress = progress == null ? null : new ReadingProgressReporter(progress, filename);
    }

    @Override
    protected Stream<String> compute(Partition partition) {
        TextFilePartition textPartition = (TextFilePartition) partition;
       
        int reportBatchSize = 64;
        try {
            FileInputStream stream = new FileInputStream(new File(path));
            FileChannel channel = stream.getChannel();
            if (textPartition.getIndex() > 0) {
                channel.position(textPartition.start);
            }
            InputStreamReader reader = new InputStreamReader(stream); // TODO pass charset
            LineNumberReader linesReader = new LineNumberReader(reader);
            
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
                        currentPosition = channel.position();
                        if (!nextLineAttempted && currentPosition <= textPartition.getEnd()) {
                            nextLine = linesReader.readLine();
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
                            reader.close();
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
    
    protected static class TextFilePartition implements Partition {
        
        private final int index;
        private final long start;
        private final long end;

        /**
         * Represents a split in an uncompressed text file.
         * 
         * @param index position of the split in the file
         * @param start starting byte where to read from in the file
         * @param end   first byte not to be read after the end of the file
         */
        protected TextFilePartition(int index, long start, long end) {
            this.index = index;
            this.start = start;
            this.end = end;
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
