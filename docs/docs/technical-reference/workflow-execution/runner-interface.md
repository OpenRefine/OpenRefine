---
id: runner-interface
title: Runner interface
sidebar_label: Runner interface
---

## Overview

The interface between runners and the application logic principally consists of the following interfaces:
* `DatamodelRunner`, which is the main entry point of the implementation and lets instantiate other classes;
* `GridState`, which represents the state of the project at some point in a workflow;
* `ChangeData`, which represents some data fetched or generated during an operation, which is serialized separately. It can be joined with a `GridState` to generate a new state incorporating this change data.

Both `GridState` and `ChangeData` are immutable: the grid or change data they represent cannot be modified through
these interfaces. Instead, to transform them, one derives new instances of those interfaces.

## Stability

No stability guidelines have been decided for these interfaces yet.

## Main operations

We give an overview of the main methods offered by the interfaces. Many operations are duplicated in two versions, a row-based one and a record-based one. This lets the implementations decide on their own strategy to deal with this duality, depending on how the grid is accessed.

### Creating grids and change data

Grids can be created in three ways:
- with `DatamodelRunner.create` by taking an existing list of rows, together with a column model and overlay models. This implies that all the rows in the grid have been loaded in memory beforehand, so this can represent a performance bottleneck.
- with `DatamodelRunner.textFile` by reading a text file. This creates a one-column grid where each line of the file becomes a row of the grid. This can be used as a basis for importers which work on text-based formats.
- with `DatamodelRunner.loadGridState` by reading a serialized grid, in our own format. This is useful to load an existing project, for instance.

Similar methods exist to load `ChangeData` objects.

### Accessing grid data

The rows in a grid can be read in various ways.
- by accessing individual rows or records by numerical id, with `getRow` or `getRecord`. If this will be repeated many times it might be more efficient to use one of the other methods below instead.
- by pagination, with the `getRows` or `getRecords` methods.
- by iterating through the entire grid, with `iterateRows` or `iterateRecords`. This method can generally be implemented efficiently by the runner, by streaming rows from their underlying storage.
- by collecting all the rows or records in a list, with `collectRows` or `collectRecords`. This should obviously be avoided for large grids.

### Gathering statistics on grids

It is possible to run aggregations on grids. The most prominent use of this for users is to compute facets, but it is also needed to compute reconciliation statistics for instance.
- `rowCount` and `recordCount` give the total number of elements in the grid. Their values will generally be cached by performance-aware implementations, so they can be called freely.
- `countMatchingRows` and `countMatchingRecords` can be used to count the number of elements matching certain conditions in the grid. This will be recomputed at every call.
- `aggregateRows` and `aggregateRecords` can be used to compute statistics, for instance in facets. To make their implementation efficient they rely on an associative sum function.

To run an aggregation, one needs to provide the following objects:
- an initial aggregation state, which will be the result of the aggregation if there are no elements to aggregate;
- a scanning function `withRow` (or `withRecord`), which is used to update the aggregation state after seeing one row
- a combining function `combine`, which combines two aggregation states into one.
These functions are required to satisfy the following properties:
- the initial aggregation state is a neutral element for the combining function: `combine(initialState, x) == x`
- the combining function is associative `combine(combine(a, b), c) == combine(a, combine(b, c))` and unital: `combine(initialState, a) == a == combine(a, initialState)`
- the scanning function and the combine function associate as well: `combine(a, withRow(b, r)) == withRow(combine(a, b), r)`
Thanks to these properties, runners can use various strategies to compute the aggregation, with the guarantee that they will reach the same final aggregation state. For instance, runners which divide the grid into partitions (such as [the local
runner](local-runner) or [the Spark runner](spark-runner)) will compute the aggregation by scanning each partition in parallel, and combining the aggregation results afterwards:

![Partitioned aggregation strategy](/img/partitioned-aggregation.svg)

Thanks to the properties required above, this strategy gives the same result as a simple sequential scan on the entire grid:

![Sequential aggregation strategy](/img/sequential-aggregation.svg)

These methods also have their approximate variants. Those methods are meant to provide similar results, but with a hard bound on the maximum number of rows or records that they should scan. This makes them return quicker at the expense of exactness. The
general idea behind these methods is that if elements are randomly sorted then this gives a form of efficient sampling. However, implementations are not required to offer a statically sound sampling method, so results should not be considered accurate
statistical estimates.
- `countMatchingRowsApprox` and `countMatchingRecordsApprox` return the number of elements matching a certain condition, capped by a user-provided limit. The returned object also indicates how many elements were actually processed.
- `aggregateRowsApprox` and `aggregateRecordsApprox` offer similar functionality for aggregations. It is recommended that the aggregator keeps track of how many elements it has seen (by adding the corresponding field in the aggregation state), so that
  aggregation results can be interpreted as proportions accordingly.

The local runner and the Spark runner both implement these methods by looking at the first few elements of each partition only, as these are the easiest ones to retrieve.

### Transforming grids

Many data transformation operations are available in the `GridState` interface. Again, they are often available in two versions, one for rows and one for records. We focus on the row-based versions for simplicity - see the javadocs for full details.
- `mapRows`: applies a function on each row, returning the new row
- `flatMapRows`: similar, but the function can return multiple rows to replace the original one
- `scanMapRows`: similar to mapRows, except that the mapping function can access and update a state as it scans the rows. The state updates are required to be associative, similarly to aggregations;
- `reorderRows`: sorts the grid using following some criteria;
- `removeRows`: delete rows which match a certain condition;
- `limitRows`: cap the grid to a certain size, removing other rows;
- `dropRows`: removes the first few rows in the grid;
- `concatenate`: concatenates the rows in two grids together;
- `withColumnModel` and `withOverlayModel` can be used to change these components of the grid.

In addition, it is possible to generate `ChangeData` objects from a grid, and join them back with the grid to obtain a new grid. For instance, to fetch contents from URLs, the process looks like this:
- create a `ChangeData` object which contains the contents of the URLs, using `mapRows` with a `RowChangeDataProducer`.
- serialize this `ChangeData` to disk, ensuring that all values are fetched
- create the new grid with the `join` method and a `RowChangeDataJoiner`, which integrates the change data into each row.
For runners which rely on partitioning, the joining process is made efficient by the fact that the partitioning on the original grid is inherited by the change data, meaning that joining them can be done by scanning the corresponding partitions
simultaneously.

### Writing out grids and change data

Grids can be serialized to disk using the `saveToFile` method. To export it to another format, exporters
should generally rely iterate on rows and generate corresponding file from that.

### Memory management

Grids can be cached in memory. This can be useful when the computation to derive it from the original data is expensive. Caching a grid will generally also benefit grids derived from it, since they will themselve be able to be computed quicker from the
pre-cached data.

### Serializability requirements

Most of the arguments accepted by the methods described above are required to be serializable using native Java serialization. This requirement is part of the interface to make it possible to implement distributed runners such as the Spark-based one. This property is enforced in the test runner so that consumers do not get bad surprises when migrating to distributed runners.
