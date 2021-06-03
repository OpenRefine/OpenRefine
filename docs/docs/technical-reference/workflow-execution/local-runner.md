---
id: local-runner
title: Local runner
sidebar_label: Local runner
---

## Overview

The local runner is the default one, as it is designed to be efficient in OpenRefine's intended usage conditions: running locally on the machine where the data cleaning is being done. Its design is inspired by Spark. Spark itself could not be used in
place of this runner because its support for distributed computations and redundancy adds significant overheads which make the tool less responsive when run locally.

## Partitioned Lazy Lists

Partitioned Lazy Lists (PLL) are a lightweight version of Spark's [Resilient Distributed Datasets (RDD)](https://spark.apache.org/docs/latest/rdd-programming-guide.html). They are:

- lists, because they represent ordered collections of objects
- lazy, because by default they do not store their contents as explicit objects in memory. Instead, the elements are computed on-demand, when they are accessed.
- partitioned, because they divide their contents into contiguous groups of elements called partitions. Each partition can be enumerated from independently, making it possible to run processes in parallel on different parts of the list.

In contrast to RDDs, PLLs are:

- not distributed: all of the data must be locally accessible, all the computations are happening in the same JVM
- not resilient: there is no support for redundancy.

The concurrency in PLLs is implemented with Java threads. When instantiated, the local runner starts a thread pool which is used on demand when [computations](runner-interface#main-operations) are executed.

## Runner architecture

With this runner, grids are represented by PLLs of rows, which can be grouped into records.
Data transformations are forwarded to the PLL API, which basically mirrors the GridState interface.
