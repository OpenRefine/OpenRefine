
package org.openrefine.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import org.openrefine.model.Grid;
import org.openrefine.model.Runner;
import org.openrefine.runners.local.LocalRunner;

public class GridSerializationBenchmark {

    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        @Param({ "100" })
        public int iterations;

        public Runner runner;
        public File path;
        public File tmpDir;
        public File targetPath;
        public Grid grid;

        @Setup(Level.Invocation)
        public void setUp() throws IOException {
            runner = new LocalRunner();
            path = new File("benchmark/testgrid");
            tmpDir = new File("/tmp/orbenchmark");
            if (tmpDir.exists()) {
                try {
                    FileUtils.deleteDirectory(tmpDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            tmpDir.mkdir();
            targetPath = new File(tmpDir, "grid");
            grid = runner.loadGrid(path);
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            ((LocalRunner) runner).getPLLContext().getExecutorService().shutdown();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    public void serializeGrid(GridSerializationBenchmark.ExecutionPlan plan, Blackhole blackhole) throws IOException {
        plan.grid.saveToFile(plan.targetPath);
    }
}
