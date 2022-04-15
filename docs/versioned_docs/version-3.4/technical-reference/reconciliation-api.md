---
id: reconciliation-api
title: Reconciliation API
sidebar_label: Reconciliation API
---

_This page is kept for the record. [A cleaner version of this specification](https://reconciliation-api.github.io/specs/0.1/) was written by the [W3C Entity Reconciliation Community Group](https://www.w3.org/community/reconciliation/), which has been formed to improve and promote this API. Join the community group to get involved!_

_This is a technical description of the mechanisms behind the reconciliation system in OpenRefine. For usage instructions, see [Reconciliation](/manual/reconciling)._

## Introduction {#introduction}

A reconciliation service is a web service that, given some text which is a name or label for something, and optionally some additional details, returns a ranked list of potential entities matching the criteria. The candidate text does not have to match each entity's official name perfectly, and that's the whole point of reconciliation--to get from ambiguous text name to precisely identified entities. For instance, given the text "apple", a reconciliation service probably should return the fruit apple, the Apple Inc. company, and New York city (also known as the Big Apple).

Entities are identified by strong identifiers in some particular identifier space. In the same identifier space, identifiers follow the same syntax. For example, given the string "apple", a reconciliation service might return entities identified by the strings " [Q89](https://www.wikidata.org/wiki/Q89)", "[Q312](https://www.wikidata.org/wiki/Q312)", and "[Q60](https://www.wikidata.org/wiki/Q60)", in the Wikidata ID space. Each reconciliation service can only reconcile to one single identifier space, but several reconciliation services can reconcile to the same identifier space.

OpenRefine defines a reconciliation API so that users can use the reconciliation features of OpenRefine with various databases (this API was originally developed to work with the now deprecated "[Freebase](https://en.wikipedia.org/wiki/Freebase)" API).

Informally, the main function of any reconciliation service is to find good candidates in the underlying database, given the following data:

* A string, which is normally the name or title of the entity, in some language.
* Optionally, a type which can be used to narrow down the search to entities of this type. OpenRefine does not define a particular set of acceptable types: this choice is left to the reconciliation service (see the suggest API for that).
* Optionally, a list of properties and their values, which can be used to refine the search. For instance, when reconciling a database of books, the author name or the publication date are useful bits of information that can be transferred to the reconciliation service. This information will be sent to the reconciliation service if the user binds columns to properties. Again, the notion of property is not predefined in OpenRefine: its definition depends on the reconciliation service.

A standard reconciliation service is a HTTP-based RESTful JSON-formatted API. It consists of various endpoints, each of which fulfills a specific function. Only the first one is mandatory.

* The root URL. This is the URL that users will need to add the service to OpenRefine. For instance, the Wikidata reconciliation interface in English has the following URL: [https://tools.wmflabs.org/openrefine-wikidata/en/api](https://tools.wmflabs.org/openrefine-wikidata/en/api)
* _Optional._ The suggest API, which enables autocompletion at various places in OpenRefine;
* _Optional._ The preview API, which lets users preview the reconciled items directly from OpenRefine;
* _Optional._ The data extension API, which lets users add columns from reconciled values based on the properties of the items in the reconciliation service.

The specification of each of these endpoints is given in the following sections.

## Workflow overview {#workflow-overview}

OpenRefine communicates with reconciliation services in the following way.

* The user adds the service by inputting its endpoint in the dialog. OpenRefine queries this URL to retrieve its [service and metadata](reconciliation-api#service-metadata) which contains basic metadata about the service (such as its name and the available features).
* The user selects a service to configure reconciliation using this service. OpenRefine queries the reconciliation service in [batch mode](reconciliation-api#multiple-query-mode) on the first ten items of the column to be reconciled. The reconciliation results are not presented to the user, but the types of the candidate items are aggregated and proposed to the user to restrict the matching to one of them. For example, if your service reconciles both people and companies, and the query for the first 10 names returns 15 candidates which are people and 7 candidates which are companies, the types will be presented to the user in that order. They can override the order and pick whichever type they want, as well as chosen a different type by hand or choose to reconcile without any type information.
* The user configures the reconciliation. If a [suggest service](reconciliation-api#suggest-apis) is available, it will be used to provide auto-completion in the dialog, to choose types or properties.
* When reconciliation starts, OpenRefine queries the service in [batch mode](reconciliation-api#multiple-query-mode) for small batches of rows and stores the responses of the service.
* Once reconciliation is complete, the results are displayed. The user makes reconciliation decisions based on the choices provided. If a [suggest service](reconciliation-api#suggest-apis) is available, it will be used to input custom reconciliation decisions. If a [preview service](reconciliation-api#preview-api) is available, the user will be able to preview the reconciliation candidates without leaving OpenRefine.

## Main reconciliation service {#main-reconciliation-service}

The root URL has two functions:

* it returns the [service and metadata](reconciliation-api#service-metadata) if no query is provided.
* when given the `queries` parameter, it performs a [set of queries in batch mode](reconciliation-api#multiple-query-mode) and returns the results for each query. This makes it efficient

There is a deprecated "single query" mode which is used if the `query` parameter is given. This mode is no longer supported or used by OpenRefine and other API consumers should not rely on it.

### Service metadata {#service-metadata}

When a service is called with just a JSONP `callback` parameter and no other parameters, it must return its _service metadata_ as a JSON object literal with the following fields:

* `"name"`: the name of the service, which will be used to display the service in the reconciliation menu;
* `"identifierSpace"`: an URI for the type of identifiers returned by the service;
* `"schemaSpace"`: an URI for the type of types understood by the service.
* `"view"` an object with a template URL to view a given item from its identifier: `"view": {"url":"http://example.com/object/{{id}}"} ` 

The last two parameters are mainly useful to assert that the identifiers returned by two different reconciliation services mean the same thing. Other fields are optional: they are used to specify the URLs for the other endpoints (suggest, preview and extend) described in the next sections.

Here are two live examples:

1. [https://tools.wmflabs.org/openrefine-wikidata/en/api](https://tools.wmflabs.org/openrefine-wikidata/en/api)
2. [http://refine.codefork.com/reconcile/viaf](http://refine.codefork.com/reconcile/viaf)

```json
{
  "name" : "Wikidata Reconciliation for OpenRefine (en)",
  "identifierSpace" : "http://www.wikidata.org/entity/",
  "schemaSpace" : "http://www.wikidata.org/prop/direct/",
  "view" : {
    "url" : "https://www.wikidata.org/wiki/{{id}}"
  },
  "defaultTypes" : [],
  "preview" : {
    ...
  },
  "suggest" : {
    ...
  },
  "extend" : {
    ...
  },
}
```

## Query Request {#query-request}

### Multiple Query Mode {#multiple-query-mode}

A call to a standard reconciliation service API for multiple queries looks like this:

    http://foo.com/bar/reconcile?queries={...json object literal...}

The json object literal has zero or more key/value pairs with arbitrary keys where the value is in the same format as a single query, e.g.

    http://foo.com/bar/reconcile?queries={ "q0" : { "query" : "foo" }, "q1" : { "query" : "bar" } }

"q0" and "q1" can be arbitrary strings. They will be used to key the results returned.

For larger data, it can make sense to use POST requests instead of GET:

```shell
curl -X POST -d 'queries={ "q0" : { "query" : "foo" }, "q1" : { "query" : "bar" } }' http://foo.com/bar/reconcile
```

OpenRefine uses POST for all requests, so make sure your service supports the format above.

### **DEPRECATED** Single Query Mode {#deprecated-single-query-mode}

A call to a reconciliation service API for a single query looks like either of these:

    http://foo.com/bar/reconcile?query=...string...
    http://foo.com/bar/reconcile?query={...json object literal...}

If the query parameter is a string, then it's an abbreviation of `query={"query":...string...}`. Here are two live examples:

1. [https://tools.wmflabs.org/openrefine-wikidata/en/api?query=boston](https://tools.wmflabs.org/openrefine-wikidata/en/api?query=boston)
2. [https://tools.wmflabs.org/openrefine-wikidata/en/api?query={%22query%22:%22boston%22,%22type%22:%22Q515%22}](https://tools.wmflabs.org/openrefine-wikidata/en/api?query={%22query%22:%22boston%22,%22type%22:%22Q515%22})

### Query JSON Object {#query-json-object}

The query json object literal has a few fields

| Parameter | Description |
| --- | --- |
| "query" | A string to search for. Required. |
| "limit" | An integer to specify how many results to return. Optional. |
| "type" | A single string, or an array of strings, specifying the types of result e.g., person, product, ... The actual format of each type depends on the service (e.g., "Q515" as a Wikidata type). Optional. |
| "type\_strict" | A string, one of "any", "all", "should". Optional. |
| "properties" | Array of json object literals. Optional |

Each json object literal of the `"properties"` array is of this form

```json
{
    "p" : string, property name, e.g., "country", or
    "pid" : string, property ID, e.g., "P17" as a Wikidata property ID
    "v" : a single, or an array of, string or number or object literal, e.g., "Japan"
  }
```

A `"v"` object literal would have a single key `"id"` whose value is an identifier resolved previously to the same identity space.

Here is an example of a full query parameter:

```json
{
    "query" : "Ford Taurus",
    "limit" : 3,
    "type" : "Q3231690",
    "type_strict" : "any",
    "properties" : [
      { "p" : "P571", "v" : 2009 },
      { "pid" : "P176" , "v" : { "id" : "Q20827633" } }
    ]
  }
```

## Query Response {#query-response}
For multiple queries, the response is a JSON literal object with the same keys as in the request

```json
{
    "q0" : {
      "result" : { ... }
    },
    "q1" : {
      "result" : { ... }
    }
  }
```

Each result consists of a JSON literal object with the structure

```json
{
    "result" : [
      {
        "id" : ... string, database ID ...
        "name" : ... string ...
        "type" : ... array of strings ...
        "score" : ... double ...
        "match" : ... boolean, true if the service is quite confident about the match ...
      },
      ... more results ...
    ],
    ... potentially some useful envelope data, such as timing stats ...
  }
```

The results should be sorted by decreasing score.
The service must also support JSONP through a callback parameter ie &callback=foo.

## Preview API {#preview-api}

The preview service API (complementary to the reconciliation service API) is quite simple. Pass it an identifier and it renders information about the corresponding entity in an HTML page, which will be shown in an iframe inside OpenRefine. The given width and height dimensions tell OpenRefine how to size that iframe.

## Suggest APIs {#suggest-apis}

In the "Start Reconciling" dialog box in OpenRefine, you can specify which type of entities the column in question contains. For instance, the column might contains titles of scientific journals. But you don't know the identifier corresponding to the "scientific journal" type. So we need a suggest API that translates "scientific journal" to something like, say, "[Q5633421](https://www.wikidata.org/wiki/Q5633421)" if we're reconciling against Wikidata.

In the same dialog box, you can specify that other columns should be used to provide more details for the reconciliation. For instance, if there is a column specifying the journals ISSN (a standard identifier for serial publications), passing that data onto the reconciliation service might make reconciliation more accurate. You might want to specify how that second column is related to the column being reconciled, but you might not now how to specify "ISSN" as a precise relationship. So we need a suggest API that translates "ISSN" to something like "[P236](https://www.wikidata.org/wiki/Property:P236)" (once again using Wikidata as an example).

There is also a need for a suggest service for entities rather than just for types and properties. When a cell has no good candidate, then you would want to perform a search yourself (by clicking on "search for match" in that cell).

Each suggest API has 2 jobs to do:

* translate what the user type into a ranked list of entities (and this is similar to the core reconciliation service and might share the same implementation)
* render a flyout when an entity is moused over or highlighted using arrow keys (and this is similar to the preview API and might share the same implementation)

The metadata for each suggest API (type, property, or entity) is as follows:

```json
{
  "service_url" : "... url including only the domain ...",
  "service_path" : "... optional relative path ...",
  "flyout_service_url" : "... optional url including only the domain ...",
  "flyout_service_path" : "... optional relative path ..."
}
```

The `service_url` field is required and it should look like this: `http://foo.com`. There should be no trailing `/` at the end. The other fields are optional and have defaults if not provided:

* `service_path` defaults to `/private/suggest`
* `flyout_service_url` defaults to the provided `service_url` field
* `flyout_service_path` defaults to `/private/flyout`

Refer to [the Suggest API documentation](suggest-api) for further details.

## Data Extension {#data-extension}

From OpenRefine 2.8 it is possible to fetch values from reconcilied sources natively. This is only possible for the reconciliation endpoints that support this additional feature, described in the [Data Extension API documentation](Data-Extension-API).

## Examples {#examples}

We've cloned a number of the Refine reconciliation services as a way of providing them visibility. They can be found at [https://github.com/OpenRefine](https://github.com/OpenRefine)

Some examples of reconciliation services which have made code available include:

* [https://github.com/dergachev/redmine-reconcile](https://github.com/dergachev/redmine-reconcile) - Python & Flask implementation that just returns the given name/number with a base url prepended
* [https://github.com/okfn/helmut](https://github.com/okfn/helmut) - A generic Refine reconciliation API implementation using Python & Flask
* [https://github.com/mblwhoi/reconciliation_service_skeleton](https://github.com/mblwhoi/reconciliation_service_skeleton) - Skeleton for Standalone Python & Flask Reconciliation Service for Refine
* [https://github.com/mikejs/reconcile-demo](https://github.com/mikejs/reconcile-demo) 
* [https://github.com/rdmpage/phyloinformatics/tree/master/services) - PHP examples (reconciliation\_\*.php](https://github.com/rdmpage/phyloinformatics/tree/master/services)
* [http://lucene.apache.org/solr/) and Python Django](https://github.com/opensemanticsearch/open-semantic-entity-search-api](https://github.com/opensemanticsearch/open-semantic-entity-search-api) - Open Source REST-API for Named Entity Extraction, Normalization, Reconciliation, Recommendation, Named Entity Disambiguation and Named Entity Linking of named entities in full-text documents by SKOS thesaurus, RDF ontologies, SQL databases and lists of names (powered by [Apache Solr)
* [https://github.com/granoproject/grano-reconcile](https://github.com/granoproject/grano-reconcile) - python example
* [https://github.com/codeforkjeff/conciliator](https://github.com/codeforkjeff/conciliator) - a Java framework for creating reconciliation services over the top of existing data sources. The code includes reconciliation services layered over [the Virtual International Authority File (VIAF)](http://viaf.org), [ORCID](http://orcid.org), [the Open Library](http://openlibrary.org) and [Apache Solr](http://lucene.apache.org/solr/).
* The open-reconcile project provides a complete Java based reconciliation service which queries a SQL database. [https://code.google.com/p/open-reconcile](https://code.google.com/p/open-reconcile)
* The [RDF Extension](http://refine.deri.ie) incorporates, among other things, reconciliation support with different approaches:
  * a service to reconciliate against querying a SPARQL endpoint
  * reconcile against a provided RDF file
  * based on Apache Stanbol ([implementation details](https://github.com/fadmaa/grefine-rdf-extension/pull/59))
* [Sunlight Labs](https://github.com/sunlightlabs) implemented a reconciliation service using Piston on Django for their [Influence Explorer](https://sunlightlabs.github.io/datacommons/). [The code is available](https://github.com/sunlightlabs/datacommons/blob/master/dcapi/reconcile/handlers.py)

Also look at the [[Reconcilable Data Sources]] page for other examples of available reconciliation services that are compatible with Refine. Not all of them are open source, but they might spark some ideas.
