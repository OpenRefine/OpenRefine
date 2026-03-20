/*******************************************************************************
 * Copyright (C) 2026, OpenRefine contributors
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

public class ApacheLevenshteinBenchmark {

    private static final Path LARGE_DATASET_PATH = Path.of("/main/tests/data/acm_large.txt");
    private static final Path CONTRACTS_DATASET_PATH = Path.of("main/tests/data/government_contracts.csv");
    private static final int MIN_DATASET_SIZE = 5000;

    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        public String left;
        public String right;
        public edu.mit.simile.vicino.distances.LevenshteinDistance vicinoDistance;
        public org.apache.commons.text.similarity.LevenshteinDistance apacheDistance;
        public List<String> dataset;
        private final Random rnd = new Random(42L);

        @Setup(Level.Trial)
        public void setUpDataset() {
            dataset = loadDataset();
            if (dataset.size() < MIN_DATASET_SIZE) {
                dataset = generateSyntheticDataset(MIN_DATASET_SIZE, rnd);
            }
        }

        @Setup(Level.Invocation)
        public void setUp() {
            vicinoDistance = new edu.mit.simile.vicino.distances.LevenshteinDistance();
            apacheDistance = new org.apache.commons.text.similarity.LevenshteinDistance();
            int idx = rnd.nextInt(dataset.size());
            left = dataset.get(idx);
            right = dataset.get((idx + 1) % dataset.size());
        }

        private static List<String> loadDataset() {
            String explicitPath = System.getProperty("openrefine.benchmark.dataset");
            if (explicitPath != null && !explicitPath.isBlank()) {
                List<String> explicitDataset = loadFromPath(Path.of(explicitPath));
                if (explicitDataset.size() >= MIN_DATASET_SIZE) {
                    return explicitDataset;
                }
            }

            List<String> largeDataset = loadLargeDataset();
            if (largeDataset.size() >= MIN_DATASET_SIZE) {
                return largeDataset;
            }

            return loadContractorNames();
        }

        private static List<String> loadFromPath(Path path) {
            if (path.getFileName() != null && path.getFileName().toString().endsWith(".csv")) {
                return loadContractorNames(path);
            }
            return loadLargeDataset(path);
        }

        private static List<String> loadLargeDataset() {
            return loadLargeDataset(LARGE_DATASET_PATH);
        }

        private static List<String> loadLargeDataset(Path path) {
            List<String> names = new ArrayList<>();
            if (!Files.exists(path)) {
                return names;
            }

            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String[] parts = line.split("\\t", 4);
                    if (parts.length > 2 && !parts[2].isBlank()) {
                        names.add(parts[2].trim());
                    }
                    if (names.size() >= MIN_DATASET_SIZE) {
                        break;
                    }
                }
            } catch (IOException e) {
                return new ArrayList<>();
            }

            return names;
        }

        private static List<String> loadContractorNames() {
            return loadContractorNames(CONTRACTS_DATASET_PATH);
        }

        private static List<String> loadContractorNames(Path path) {
            List<String> names = new ArrayList<>();
            if (!Files.exists(path)) {
                return names;
            }

            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String[] parts = line.split(",", 3);
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        names.add(parts[1].trim());
                    }
                    if (names.size() >= MIN_DATASET_SIZE) {
                        break;
                    }
                }
            } catch (IOException e) {
                return new ArrayList<>();
            }

            return names;
        }

        private static List<String> generateSyntheticDataset(int size, Random random) {
            List<String> names = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                names.add(randomWord(18, random));
            }
            return names;
        }

        private static String randomWord(int length, Random random) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append((char) ('A' + random.nextInt(26)));
            }
            return sb.toString();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    public void vicinoLevenshtein(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.vicinoDistance.d(plan.left, plan.right));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    public void apacheLevenshtein(ExecutionPlan plan, Blackhole blackhole) {
        blackhole.consume(plan.apacheDistance.apply(plan.left, plan.right));
    }
}
