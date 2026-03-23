/*******************************************************************************
 * Copyright (C) 2025, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.benchmark;

import java.util.concurrent.TimeUnit;

import edu.mit.simile.vicino.distances.PPMDistance;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.google.refine.clustering.knn.DeflateNCDDistance;
import com.google.refine.clustering.knn.VicinoDistance;

public class DeflateNCDBenchmark {

    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        public DeflateNCDDistance deflateDistance;
        public VicinoDistance vicinoDistance;

        public String shortA = "New York";
        public String shortB = "new york";

        public String mediumA = "University of California, Berkeley";
        public String mediumB = "University of California Berkeley";

        public String longA = "International Business Machines Corporation Headquarters";
        public String longB = "International Business Machine Corporation Headquarter";

        @Setup(Level.Trial)
        public void setUp() {
            deflateDistance = new DeflateNCDDistance();
            vicinoDistance = new VicinoDistance(new PPMDistance());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void deflateShortStrings(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.deflateDistance.compute(plan.shortA, plan.shortB));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void vicinoPPMShortStrings(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.vicinoDistance.compute(plan.shortA, plan.shortB));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void deflateMediumStrings(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.deflateDistance.compute(plan.mediumA, plan.mediumB));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void vicinoPPMMediumStrings(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.vicinoDistance.compute(plan.mediumA, plan.mediumB));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void deflateLongStrings(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.deflateDistance.compute(plan.longA, plan.longB));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void vicinoPPMLongStrings(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.vicinoDistance.compute(plan.longA, plan.longB));
    }
}
