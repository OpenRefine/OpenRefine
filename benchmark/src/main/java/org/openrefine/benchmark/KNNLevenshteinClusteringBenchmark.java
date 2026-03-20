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

import edu.mit.simile.vicino.distances.LevenshteinDistance;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.google.refine.browsing.Engine;
import com.google.refine.clustering.knn.ApacheLevenshteinDistance;
import com.google.refine.clustering.knn.DistanceFactory;
import com.google.refine.clustering.knn.VicinoDistance;
import com.google.refine.clustering.knn.kNNClusterer;
import com.google.refine.clustering.knn.kNNClusterer.kNNClustererConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class KNNLevenshteinClusteringBenchmark {

    private static final Path LARGE_DATASET_PATH = Path.of("main/tests/data/acm_large.txt");
    private static final Path CONTRACTS_DATASET_PATH = Path.of("main/tests/data/government_contracts.csv");
    private static final int MIN_DATASET_SIZE = 5000;
    private static final String VALUE_COLUMN = "value";
    private static final String VICINO_NAME = "benchmark-vicino-levenshtein";
    private static final String APACHE_NAME = "benchmark-apache-levenshtein";

    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        @Param({ "200", "500" })
        public int rowCount;

        public List<String> values;

        @Setup(Level.Trial)
        public void setUp() {
            List<String> dataset = loadDataset();
            if (dataset.size() < rowCount) {
                dataset = generateSyntheticDataset(Math.max(rowCount, MIN_DATASET_SIZE));
            }
            values = dataset.subList(0, rowCount);

            DistanceFactory.remove(VICINO_NAME);
            DistanceFactory.remove(APACHE_NAME);
            DistanceFactory.put(VICINO_NAME, new VicinoDistance(new LevenshteinDistance()));
            DistanceFactory.put(APACHE_NAME, new ApacheLevenshteinDistance());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void vicinoKNNClustering(ExecutionPlan plan, Blackhole blackhole) throws IOException {
        blackhole.consume(runKnnClustering(plan.values, VICINO_NAME));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void apacheKNNClustering(ExecutionPlan plan, Blackhole blackhole) throws IOException {
        blackhole.consume(runKnnClustering(plan.values, APACHE_NAME));
    }

    private int runKnnClustering(List<String> values, String distanceName) throws IOException {
        Project project = createProject(values);
        Engine engine = new Engine(project);
        kNNClustererConfig config = ParsingUtilities.mapper.readValue(makeConfigJson(distanceName), kNNClustererConfig.class);
        kNNClusterer clusterer = config.apply(project);
        clusterer.computeClusters(engine);
        return clusterer.getJsonRepresentation().size();
    }

    private Project createProject(List<String> values) {
        Project project = new Project();
        int cellIndex = project.columnModel.allocateNewCellIndex();

        try {
            project.columnModel.addColumn(cellIndex, new Column(cellIndex, VALUE_COLUMN), true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize benchmark column", e);
        }

        for (String value : values) {
            Row row = new Row(1);
            row.setCell(cellIndex, new Cell(value, null));
            project.rows.add(row);
        }

        project.columnModel.update();
        project.recordModel.update(project);
        return project;
    }

    private static String makeConfigJson(String distanceName) {
        return "{" +
                "\"type\":\"knn\"," +
                "\"function\":\"" + distanceName + "\"," +
                "\"column\":\"" + VALUE_COLUMN + "\"," +
                "\"params\":{\"radius\":1,\"blocking-ngram-size\":2}" +
                "}";
    }

    private static List<String> loadDataset() {
        String explicitPath = System.getProperty("openrefine.benchmark.dataset");
        if (explicitPath != null && !explicitPath.isBlank()) {
            List<String> explicitDataset = loadFromPath(Path.of(explicitPath));
            if (explicitDataset.size() >= MIN_DATASET_SIZE) {
                return explicitDataset;
            }
        }

        List<String> largeDataset = loadLargeDataset(LARGE_DATASET_PATH);
        if (largeDataset.size() >= MIN_DATASET_SIZE) {
            return largeDataset;
        }

        return loadContractorNames(CONTRACTS_DATASET_PATH);
    }

    private static List<String> loadFromPath(Path path) {
        if (path.getFileName() != null && path.getFileName().toString().endsWith(".csv")) {
            return loadContractorNames(path);
        }
        return loadLargeDataset(path);
    }

    private static List<String> loadLargeDataset(Path path) {
        List<String> values = new ArrayList<>();
        if (!Files.exists(path)) {
            return values;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split("\\t", 4);
                if (parts.length > 2 && !parts[2].isBlank()) {
                    values.add(parts[2].trim());
                }
                if (values.size() >= MIN_DATASET_SIZE) {
                    break;
                }
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }

        return values;
    }

    private static List<String> loadContractorNames(Path path) {
        List<String> values = new ArrayList<>();
        if (!Files.exists(path)) {
            return values;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",", 3);
                if (parts.length > 1 && !parts[1].isBlank()) {
                    values.add(parts[1].trim());
                }
                if (values.size() >= MIN_DATASET_SIZE) {
                    break;
                }
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }

        return values;
    }

    private static List<String> generateSyntheticDataset(int size) {
        Random random = new Random(42L);
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(randomWord(18, random));
        }
        return values;
    }

    private static String randomWord(int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        return sb.toString();
    }
}
