
package org.openrefine.model.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobContext;
import org.apache.hadoop.mapred.JobContextImpl;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;

/**
 * A PLL whose contents are read from a set of text files. The text files are partitioned using Hadoop, using new lines
 * as boundaries.
 * 
 * This class aims at producing a certain number of partitions determined by the default parallelism of the PLL context.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TextFilePLL extends PLL<String> {

    private final List<HadoopPartition> partitions;
    private final String path;
    private final PLLContext context;
    private final InputFormat<LongWritable, Text> inputFormat = new TextInputFormat();

    public TextFilePLL(PLLContext context, String path) throws IOException {
        super(context);
        this.path = path;
        this.context = context;

        FileSystem fs = context.getFileSystem();

        // Setup the job to compute the splits
        Configuration conf = new Configuration(fs.getConf());
        Job job = Job.getInstance(conf);
        FileInputFormat.setInputPaths(job, path);
        JobID jobId = new JobID();
        JobContext jobContext = new JobContextImpl((JobConf) job.getConfiguration(), jobId);

        List<InputSplit> splits;
        partitions = new ArrayList<>();
        try {
            // First attempt to get splits using the default parameters
            splits = inputFormat.getSplits(jobContext);

            // If there are too few splits compared to the default parallelism,
            // and at least one split is large enough to be split again, then
            // we split again with lower maximum split size.
            if (splits.size() < context.getDefaultParallelism()) {
                long maxSplitSize = 0;
                for (InputSplit split : splits) {
                    maxSplitSize = Math.max(split.getLength(), maxSplitSize);
                }
                if (maxSplitSize > context.getMinSplitSize() * context.getDefaultParallelism()) {
                    // re-split with lower maximum split size
                    long newMaxSplitSize = maxSplitSize / context.getDefaultParallelism();
                    conf.set("mapreduce.input.fileinputformat.split.maxsize", Long.toString(newMaxSplitSize));
                    job.close();
                    job = Job.getInstance(conf);
                    FileInputFormat.setInputPaths(job, path);
                    jobContext = new JobContextImpl((JobConf) job.getConfiguration(), jobId);
                    splits = inputFormat.getSplits(jobContext);
                }
            }

            for (int i = 0; i != splits.size(); i++) {
                partitions.add(new HadoopPartition(i, splits.get(i)));
            }
        } catch (InterruptedException e) {
            partitions.clear();
            e.printStackTrace();
        }

    }

    @Override
    protected Stream<String> compute(Partition partition) {
        HadoopPartition hadoopPartition = (HadoopPartition) partition;
        TaskAttemptID attemptId = new TaskAttemptID();
        TaskAttemptContext taskAttemptContext = new TaskAttemptContextImpl(context.getFileSystem().getConf(), attemptId);
        try {
            RecordReader<LongWritable, Text> reader = inputFormat.createRecordReader(hadoopPartition.getSplit(), taskAttemptContext);
            reader.initialize(hadoopPartition.getSplit(), taskAttemptContext);
            Iterator<String> iterator = new Iterator<String>() {

                boolean finished = false;
                boolean havePair = false;

                @Override
                public boolean hasNext() {
                    if (!finished && !havePair) {
                        try {
                            finished = !reader.nextKeyValue();
                        } catch (IOException | InterruptedException e) {
                            finished = true;
                            e.printStackTrace();
                        }
                        havePair = !finished;
                    }
                    return !finished;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("End of stream");
                    }
                    String line = null;
                    try {
                        line = reader.getCurrentValue().toString();
                    } catch (IOException | InterruptedException e) {
                        finished = true;
                        e.printStackTrace();
                    }
                    havePair = false;
                    return line;
                }

            };
            Stream<String> stream = Streams.stream(iterator)
                    .onClose(() -> {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            return stream;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    @Override
    public List<? extends Partition> getPartitions() {
        return partitions;
    }

    protected static class HadoopPartition implements Partition {

        private final int index;
        private final InputSplit split;

        protected HadoopPartition(int index, InputSplit split) {
            this.index = index;
            this.split = split;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Partition getParent() {
            return null;
        }

        protected InputSplit getSplit() {
            return split;
        }

    }

}
