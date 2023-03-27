
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
import org.openrefine.runners.testing.TestingRunner;

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
            runner = new TestingRunner(); // new LocalRunner();
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
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            if (runner instanceof LocalRunner) {
                ((LocalRunner) runner).getPLLContext().getExecutorService().shutdown();
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    public void collectGrid(GridSerializationBenchmark.ExecutionPlan plan, Blackhole blackhole) throws IOException {
        Grid grid = plan.runner.loadGrid(plan.path);
        blackhole.consume(grid.collectRows());
    }
}
