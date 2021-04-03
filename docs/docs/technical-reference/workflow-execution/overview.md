---
id: workflow-execution-overview
title: Overview of the workflow execution architecture
sidebar_label: Overview of the architecture
---

Workflows are series of data transformation steps done inside OpenRefine: importing, transforming and exporting projects. Our architecture provides a separation between the transformation logic (such as the definition of the operations executed) and a
lower-level layer, the runner, which deals with the logistics: how data is stored, indexed, when it is loaded in memory, and so on. OpenRefine comes with multiple pre-defined runners which are suitable for various use cases.

## Available runners

- The [local runner](local-runner) is the default one. It is designed to be run when all of the data to transform is located on the same machine (where OpenRefine is running). Project data is read from disk in a lazy fashion, i.e. only when the corresponding grid values need  to be displayed, aggregated or exported. Therefore it makes it possible to run OpenRefine on large datasets without the need for a large working memory (RAM).

- The [Spark runner](spark-runner) is designed to run on distributed datasets. Those datasets can be split into blocks which are stored on different machines. The execution of the workflow can be shared between the various machines, which form a Spark cluster.

- The testing runner is a very simple runner, which loads all the data it works on in memory. It is not optimized for performance at all: it simply meets the specification of the runner interface in the simplest way possible. It is used in tests of operations, importers and other basic blocks of workflows. Its simplicity makes it fast enough on small testing datasets generally used in such tests. This runner also performs additional checks during execution, so that incorrect behaviours can be detected more easily during testing.

## Runner selection

The runner is currently instantiated in the `RefineServlet` class. It is foreseen that we introduce command-line options to let the user select which runner an OpenRefine instance uses, but it is not currently configurable.

## Creating a new runner

Developing a new runner is as simple as implementing [the `DatamodelRunner` interface](runner-interface).

We offer a general test suite that all runners should pass (`DatamodelRunnerTestBase`), you can run it on your
runner by subclassing it.

