# OpenRefine Benchmarks (JMH)

This module contains [Java Microbenchmark Harness (JMH)](https://openjdk.org/projects/code-tools/jmh/) benchmarks for OpenRefine.

## Prerequisites

- Java (JDK 11+)
- Maven

## Build The Benchmark JAR

From the repository root:

```bash
mvn -pl benchmark -am -DskipTests package
```

This produces:

- `benchmark/target/openrefine-benchmarks.jar`

## List Available Benchmarks

```bash
java -jar benchmark/target/openrefine-benchmarks.jar -l
```

## General JMH Controls

Useful optional flags:

- `-f <n>`: forks
- `-wi <n>`: warmup iterations
- `-i <n>`: measurement iterations
- `-w <duration>`: warmup time per iteration (for example `200ms`)
- `-r <duration>`: measurement time per iteration (for example `300ms`)
- `-t <n>`: threads
- `-p name=value`: benchmark parameter override

## Benchmarks In This Module

### ToNumberBenchmark

Class:

- `org.openrefine.benchmark.ToNumberBenchmark`

Methods:

- `toDoubleNew`
- `toLongNew`

Built-in parameter:

- `iterations`: `1000`, `10000`

Examples:

```bash
# Run the whole class
java -jar benchmark/target/openrefine-benchmarks.jar ToNumberBenchmark

# Run one method
java -jar benchmark/target/openrefine-benchmarks.jar ToNumberBenchmark.toLongNew

# Override parameter
java -jar benchmark/target/openrefine-benchmarks.jar ToNumberBenchmark -p iterations=10000
```

### ApacheLevenshteinBenchmark (distance-only comparison)

Class:

- `org.openrefine.benchmark.ApacheLevenshteinBenchmark`

Methods:

- `apacheLevenshtein`
- `vicinoLevenshtein`

Purpose:

- Compare raw Levenshtein distance computation performance between Apache Commons Text and Vicino.

Dataset selection order:

1. JVM property `openrefine.benchmark.dataset` (if provided)
2. `main/tests/data/acm_large.txt` (if present)
3. `main/tests/data/government_contracts.csv`
4. Synthetic fallback dataset

Examples:

```bash
# Run full distance comparison
java -jar benchmark/target/openrefine-benchmarks.jar ApacheLevenshteinBenchmark

# Run with explicit dataset path
java -Dopenrefine.benchmark.dataset=/root/data/itunes_amazon_tableB_large.txt \
  -jar benchmark/target/openrefine-benchmarks.jar ApacheLevenshteinBenchmark

# Run one method only
java -jar benchmark/target/openrefine-benchmarks.jar ApacheLevenshteinBenchmark.apacheLevenshtein
```

### KNNLevenshteinClusteringBenchmark (end-to-end clustering)

Class:

- `org.openrefine.benchmark.KNNLevenshteinClusteringBenchmark`

Methods:

- `apacheKNNClustering`
- `vicinoKNNClustering`

Purpose:

- Compare full kNN clustering runtime, not just distance calls.

Built-in parameter (safe defaults):

- `rowCount`: `200`, `500`

Default warmup/measurement (in code):

- Warmup: 1 iteration x 200ms
- Measurement: 2 iterations x 300ms

Examples:

```bash
# Run end-to-end comparison with defaults
java -jar benchmark/target/openrefine-benchmarks.jar KNNLevenshteinClusteringBenchmark

# Run with explicit dataset and small row count
java -Dopenrefine.benchmark.dataset=/root/data/itunes_amazon_tableB_large.txt \
  -jar benchmark/target/openrefine-benchmarks.jar KNNLevenshteinClusteringBenchmark -p rowCount=200

# Extra-safe quick profile
java -jar benchmark/target/openrefine-benchmarks.jar KNNLevenshteinClusteringBenchmark \
  -f 1 -wi 0 -i 1 -r 1s -w 0s -p rowCount=200 -t 1
```

## Notes On Runtime And Thermals

- End-to-end clustering benchmarks can be CPU-intensive.
- If your machine runs hot, use:
  - lower `rowCount`
  - `-t 1`
  - fewer iterations (`-wi 0 -i 1`) for quick checks
- JMH can appear to stay on one iteration for a while whenever each operation is CPU intensive.
