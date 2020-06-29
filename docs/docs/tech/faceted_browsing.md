---
id: faceted_browsing
title: Faceted Browsing Architecture
sidebar_label: Faceted Browsing
---

Faceted browsing support is core to OpenRefine as it is the primary and only mechanism for filtering to a subset of rows on which to do something _en masse_ (ie in bulk). Without faceted browsing or an equivalent querying/browsing mechanism, you can only change one thing at a time (one cell or row) or else change everything all at once; both kinds of editing are practically useless when dealing with large data sets.

In OpenRefine, different components of the code need to know which rows to process from the faceted browsing state (how the facets are constrained). For example, when the user applies some facet selections and then exports the data, the exporter serializes only the matching rows, not all rows in the project. Thus, faceted browsing isn't only hooked up to the data view for displaying data to the user, but it is also hooked up to almost all other parts of the system.

## Engine Configuration

As OpenRefine is a web app, there might be several browser windows opened on the same project, each in a different faceted browsing state. It is best to maintain the faceted browsing state in each browser window while keeping the server side completely stateless with regard to faceted browsing. Whenever the client-side needs something done by the server, it transfers the entire faceted browsing state over to the server-side. The faceted browsing state behaves much like the `WHERE` clause in a SQL query, telling the server-side how to select the rows to process.

In fact, it is best to think of the faceted browsing state as just a database query much like a SQL query. It can be passed around the whole system, to any component needing to know which rows to process. It is serialized into JSON to pass between the client-side and the server-side, or to save in an abstract operation's specification. The job of the faceted browsing subsystem on the client-side is to let the user interactively modify this "faceted browsing query", and the job of the faceted browsing subsystem on the server-side is to resolve that query.

In the code, the faceted browsing state, or faceted browsing query, is actually called the *engine configuration* or *engine config* for short. It consists mostly of an array facet configurations. For each facet, it stores the name of the column on which the facet is based (or an empty string if there is no base column). Each type of facet has different configuration. Text search facets have queries and flags for case-sensitivity mode and regular expression mode. Text facets (aka list facets) and numeric range facets have expressions. Each list facet also has an array of selected choices, an invert flag, and flags for whether blank and error cells are selected. Each numeric range facet has, among other things, a "from" and a "to" values. If you trace the AJAX calls, you'd see the engine configs being shuttled, e.g.,

```json
{
    "facets" : [
      {
        "type": "text",
        "name": "Shrt_Desc",
        "columnName": "Shrt_Desc",
        "mode": "text",
        "caseSensitive": false,
        "query": "cheese"
      },
      {
        "type": "list",
        "name": "Shrt_Desc",
        "columnName": "Shrt_Desc",
        "expression": "grel:value.toLowercase().split(\",\")",
        "omitBlank": false,
        "omitError": false,
        "selection": [],
        "selectBlank":false,
        "selectError":false,
        "invert":false
      },
      {
        "type": "range",
        "name": "Water",
        "expression": "value",
        "columnName": "Water",
        "selectNumeric": true,
        "selectNonNumeric": true,
        "selectBlank": true,
        "selectError": true,
        "from": 0,
        "to": 53
      }
    ],
    "includeDependent": false
  }
```

## Server-Side Subsystem

From an engine configuration like the one above, the server-side faceted browsing subsystem is capable of producing:

- an iteration over the rows matching the facets' constraints
- information on how to render the facets (e.g., choice and count pairs for a list facet, histogram for a numeric range facet)

When the engine config JSON arrives in an HTTP request on the server-side, a `com.google.refine.browsing.Engine` object is constructed and initialized with that JSON. It in turns constructs zero or more `com.google.refine.browsing.facets.Facet` objects. Then for each facet, the engine calls its `getRowFilter()` method, which returns `null` if the facet isn't constrained in anyway, or a `com.google.refine.browsing.filters.RowFilter` object. Then, to when iterating over a project's rows, the engine calls on all row filters' `filterRow()` method. If and only if all row filters return `true` the row is considered to match the facets' constraints. How each row filter works depends on the corresponding type of facet.

To produce information on how to render a particular facet in the UI, the engine follows the same procedure described in the previous except it skips over the facet in question. In other words, it produces an iteration over all rows constrained by the other facets. Then it feeds that iteration to the facet in question by calling the facet's `computeChoices()` method. This gives the method a chance to compute the rendering information for its UI counterpart on the client-side. When all facets have been given a chance to compute their rendering information, the engine calls all facets to serialize their information as JSON and returns the JSON to the client-side. Only one HTTP call is needed to compute all facets.

## Client-Side Subsystem

On the client-side there is also an engine object (implemented in Javascript rather than Java) and zero or more facet objects (also in Javascript, obviously). The engine is responsible for distributing the rendering information computed on the server-side to the right facets, and when the user interacts with a facet, the facet tells the engine to update the whole UI. To do so, the engine gathers the configuration of each facet and composes the whole engine config as a single JSON object. Two separate AJAX calls are made with that engine config, one to retrieve the rows to render, and one to re-compute the rendering information for the facets because changing one facet does affect all the other facets.


