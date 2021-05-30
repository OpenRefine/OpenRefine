---
id: suggest-api
title: Suggest API
sidebar_label: Suggest API
---

The Suggest API has 2 entry points:

- `suggest`: translates some text that the user has typed, optionally constrained in some ways, to a ranked list of entities (this is very similar to a [Reconciliation API](reconciliation-api) and can share the same implementation)
- `flyout` : renders a small view of an entity

For the `suggest` entry point, it is important to balance speed versus accuracy. The widget must respond in interactive time, meaning about 200 msec, for each change to the user's text input. At the same time, the ranked list of entities must seem quite relevant to what the user types.

Similarly, for the `flyout` entry point, it is important to respond quickly while providing enough essential details so that the user can visually check if the highlighted entity is the desired one. You probably would want to embed a thumbnail image, as we have found that images are excellent for visual identification.

## suggest Entry Point {#suggest-entry-point}

The `suggest` entry point takes the following URL parameters

Parameter | Description                 | Required/Optional
----------|-----------------------------|------------------
 "prefix" | a string the user has typed | required 
 "type" | optional, a single string, or an array of strings, specifying the types of result e.g., person, product, ... The actual format of each type depends on the service | optional |
 "type\_strict" | optional, a string, one of "any", "all", "should" | optional |
 "limit" | optional, an integer to specify how many results to return | optional |
 "start" | optional, an integer to specify the first result to return (thus in conjunction with `limit`, support pagination) | optional |

The Suggest API should return results as JSON (JSONP must also be supported). The JSON should consist of a 'result' array containing objects with at least a 'name' and 'id'. The JSON response can optionally include other information as illustrated in this structure for a full JSON response:
```json
 {
    "code" : "/api/status/ok",
    "status" : "200 OK",
    "prefix" : ... string, the prefix URL parameter echoed back ...
    "result" : [
      {
        "id" : ... string, identifier of entity ...
        "name" : ... string, nameof entity ...
        "notable" : [{
          "id" : ... string, identifier of type ...
          "name" : ... string, name of type ...
        }, ...]
      },
      ... more results ...
    ],
  }
```

* `code` (optional) error/success state of the API above the level of HTTP. Use "/api/status/ok" for a successful request and "/api/status/error" if there has been an error.
* `status` (optional) should correspond to the HTTP response code.
* `prefix` (optional) the query string submitted to the Suggest API.
* `result` (required) array containing multiple results from the Suggest API consisting of at least an id and name.
* `id` (required) the id of an entity being suggested by the Suggest API
* `name` (required) a short string which labels or names the entity being suggested by the Suggest API
* `description` (optional) a short description of the item, which will be displayed below the name
* `notable` (optional) is a a list of JSON objects that describes the types of the entity. They are rendered in addition to the entity's name to provide more disambiguation details and stored in the reconciliation data of the cells. This list can also be supplied as a list of type identifiers (such as `["Q5"]` instead of `[{"id":"Q5","name":"human"}]`).

Here is an example of a minimal request and response using the Suggest API layered over [Wikidata](https://www.wikidata.org):

URL: https://tools.wmflabs.org/openrefine-wikidata/en/suggest/entity?prefix=A5
JSON response:

```json
{
  "result": [
    {
      "name": "A5",
      "description": "road",
      "id": "Q429719"
    },
    {
      "name": "Apple A5",
      "description": null,
      "id": "Q420764"
    },
    {
      "name": "A5 autoroute",
      "description": "controlled-access highway from Paris's Francilienne to the A31 near Beauchemin",
      "id": "Q788832"
    },
    ...
  ]
}
```

## flyout Entry Point {#flyout-entry-point}

The `flyout` entry point takes a single URL parameter: `id`, which is the identifier of the entity to render, as a string. It also takes a `callback` parameter to support JSONP. It returns a JSON object literal with a single field: `html`, which is the rendered view of the given entity. 

Here is an example of a minimal request and response using the Suggest API layered over [[Wikidata](https://www.wikidata.org) with only the required fields in each case:

URL: https://tools.wmflabs.org/openrefine-wikidata/en/flyout/entity?id=Q786288
JSON response:

```json
{
  "html": "<p style=\"font-size: 0.8em; color: black;\">national road in Latvia</p>",
  "id": "Q786288"
}
```

OpenRefine incorporates a set of `fbs-` CSS class names which can be used in the flyout HTML if desired to render the flyout information in a standard style. 
