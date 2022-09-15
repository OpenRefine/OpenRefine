---
id: spark-runner
title: Spark runner
sidebar_label: Spark runner
---


The Spark runner uses [Apache Spark](https://spark.apache.org/) to execute workflows. It relies on the [RDD API](https://spark.apache.org/docs/latest/rdd-programming-guide.html) to represent the grid and manipulate it.

## Options

The following configuration parameters can be used with this runner:
|Configuration key|Default value|Description|
|---|---|---|
| `refine.runner.defaultParallelism` | `4` | how many partitions datasets should generally be split, unless they are very small or very big |
| `refine.runner.sparkMasterURI` | `local[4]` | a Spark URI, such as `spark://host:port` or `mesos://host:port`, to connect to an external Spark cluster for the execution of workflows. |

## Running on Windows

To use the Spark runner on Windows, additional steps are required to provide native binaries required by Hadoop:
* Copy the Windows utilities for Hadoop on the machine where OpenRefine is installed. It is important that the version of the Hadoop utilities match that of the Hadoop dependency in the Spark runner, which is currently 3.3.1. Those binaries are not officially released by the Apache Hadoop project but can be found in third-party repositories. They should be downloaded, for instance in `C:\Hadoop`, which should contain a `bin` subdirectory with `winutils.exe` and `hadoop.dll`;
* Configure the `HADOOP_HOME` environment variable to point to this directory. In the example above, `HADOOP_HOME` should be set to `C:\Hadoop`.
* Add the `bin` subdirectory to the `PATH` environment variable. In our example, this means adding `C:\Hadoop\bin` to the `PATH` environment variable.

## Connection to external Spark clusters

By default, the Spark runner launches its own local Spark cluster when starting up. To use an existing Spark cluster instead, configure the `refine.runner.sparkMasterURI` with the address of the cluster.

In addition, the cluster needs to be populated with a `.jar` file containing the code for OpenRefine's workflow elements. Such a file will be published soon.
