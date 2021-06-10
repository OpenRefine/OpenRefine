---
id: data-extension-api
title: Data extension API
sidebar_label: Data extension API
---

This page describes a new optional API for reconciliation services, allowing clients to pull properties of reconciled records. It is supported from OpenRefine 2.8 onwards. A sample server implementation is available in the [Wikidata reconciliation interface](https://tools.wmflabs.org/openrefine-wikidata/).

## Overview of the workflow {#overview-of-the-workflow}

1. Reconcile a column with a standard reconciliation service

2. Click "Add column from reconciled values"

3. The user is proposed some properties to fetch, based on the type they reconciled their column against (if any). They can also pick their own property with the suggest widget (same as for the reconciliation dialog).

4. A preview of the columns to be fetched is displayed on the right-hand side of the dialog, based on a sample of the rows.

5. Once the user has clicked "OK", columns are fetched and added to the project. Columns corresponding to other items from the service are directly reconciled, and the column is marked as reconciled against the type suggested by the service for that
property. The user can run data extension again from that column.

[GIF Screencast](http://pintoch.ulminfo.fr/92dcdd20f3/recorded.gif)

## Specification {#specification}

Services supporting data extension must add an `extend` field in their service metadata. This field is expected to have the following subfields, all optional:
* `propose_properties` stores the endpoint of an API which will be used to suggest properties to fetch (see specification below). The field contains an object with a `service_url` and `service_path` which will be concatenated to obtain the URL where the endpoint is available, just like the other services in the metadata. If this field is not provided, no property will be suggested in the dialog (the user will have to input them manually).
* `property_settings` stores the specification of a form where the user will be able to configure how a given property should be fetched (see specification below). If this field is not provided, the user will not be proposed with settings.

The service endpoint must also accept a new parameter `extend` (in addition to `queries` which is used for reconciliation). Its behaviour is described in the following section.

Example service metadata:
```json
    "extend": {
      "propose_properties": {
        "service_url": "https://tools.wmflabs.org/openrefine-wikidata",
        "service_path": "/en/propose_properties"
      },
      "property_settings": []
    }
```
### Property proposal protocol {#property-proposal-protocol}

The role of the property proposal endpoint is to suggest a list of properties to fetch. As only input, it accepts GET parameters:
* the `type` of a column was reconciled against. If no type is provided, it should suggest properties for a column reconciled against no type.
* a `limit` on the number of results to return

The type is specified by its id in the `type` GET parameter of the endpoint, as follows:

https://tools.wmflabs.org/openrefine-wikidata/en/propose_properties?type=Q3354859&limit=3

The endpoint returns a JSON response as follows:

```json
    {
      "properties": [
        {
          "id": "P969",
          "name": "located at street address"
        },
        {
          "id": "P1449",
          "name": "nickname"
        },
        {
          "id": "P17",
          "name": "country"
        },
      ],
      "type": "Q3354859",
      "limit": 3
    }
```
This endpoint must support JSONP via the `callback` parameter (just like all other endpoints of the reconciliation service).

### Data extension protocol {#data-extension-protocol}

After calling the property proposal endpoint, the consumer (OpenRefine) calls the service endpoint with a JSON object in the `extend` parameter, containing the following fields:
* `ids` is a list of strings, each of which being an identifier of a record as returned by the reconciliation method. These are the records whose properties should be retrieved.
* `properties` is a list of JSON objects. They specify the properties to be fetched for each item, and contain the following fields:
  * `id` (a string): the identifier of the property as returned by the property suggest service (and optionally the property proposal service)
  * `settings`: a JSON object storing parameters about how the property should be fetched (see below).

Example:
```json
    {
      "ids": [
        "Q7205598",
        "Q218765",
        "Q845632",
        "Q5661356"
      ],
      "properties": [
        {
          "id": "P856"
        },
        {
          "id": "P159"
        }
      ]
    }
```
The service returns a JSON response formatted as follows:

* `meta` contains a list of column metadata. The order of the properties must be the same
   as the one provided in the query. Each element is an object containing the following keys:
   * `id` (mandatory): the identifier of the property
   * `name` (mandatory): the human-readable name of the property
   * `type` (optional): an object with `id` and `name` keys representing the expected
      type of values for that property. The notion of type is the same as the one
      used for reconciliation. The `type` field should only be provided when the property returns
      reconciled items.
* `rows` contains an object. Its keys must be exactly the record ids (`ids`) passed in the query.
   The value for each record id is an object representing a row for that id. The keys of a    row object must be exactly the property ids passed in the query (`"P856"` and `"P159"` in the example above). The value for a property id should be a list of cell objects.

Cell objects are JSON objects which contain the representation of an OpenRefine cell.
* an object with a single `"str"` key and a string value for it represents
  a cell with a (bare) string in it.
  Example: `{"str": "193.54.0.0/15"}`

* an object with `"id"` and `"name"` represents a reconciled value
  (from the same reconciliation service). It will be stored as 
  a matched cell (with maximum reconciliation score).
  Example: `{"name": "Warsaw","id": "Q270"}`

* an empty object `{}` represents an empty cell

* an object with `"date"` and an ISO-formatted date string represents a point in time.
  Example: `{"date": "1987-02-01T00:00:00+00:00"}`

* an object with `"float"` and a numerical value represents a quantity.
  Example: `{"float": 48.2736}`

* an object with `"int"` and an integer represents a number.
  Example: `{"int": 54}`

* an object with `"bool"` and `true` or `false` represents a boolean.
  Example: `{"bool": false}`

Example of a full response (for the example query above):
```json
    {
      "rows": {
        "Q5661356": {
          "P159": [],
          "P856": []
        },
        "Q7205598": {
          "P159": [
            {
              "name": "Warsaw",
              "id": "Q270"
            }
          ],
          "P856": [
            {
              "str": "http://www.polkomtel.com.pl/english"
            },
            {
              "str": "http://www.polkomtel.com.pl/"
            }
          ]
        },
        "Q845632": {
          "P159": [
            {
              "name": "Bærum",
              "id": "Q57076"
            }
          ],
          "P856": [
            {
              "str": "http://www.telenor.com/"
            }
          ]
        },
        "Q218765": {
          "P159": [
            {
              "name": "Paris",
              "id": "Q90"
            }
          ],
          "P856": [
            {
              "str": "http://www.sfr.fr/"
            }
          ]
        }
      },
      "meta": [
        {
          "id": "P159",
          "name": "headquarters location",
          "type": {
             "id": "Q7540126",
             "name": "headquarters",
          }
        },
        {
          "id": "P856",
          "name": "official website",
        }
      ]
    }
```
### Settings specification {#settings-specification}

The `property_settings` field in the service metadata allows the service to declare it accepts some settings for the properties it fetches. They are specified as a list of JSON objects which define the fields which should be exposed to the user.

Each setting object looks like this:
```json
      {
        "default": 0,
        "type": "number",
        "label": "Limit",
        "name": "limit",
        "help_text": "Maximum number of values to return per row (0 for no limit)"
      }
```
It is essentially a definition of a form field in JSON, with self-explanatory fields.
The `type` field specifies the type of the form field (among `number`, `select`, `text`, `checkbox`).
The field `default` gives the default value of the form: the service must assume this value if the
client does not specify this setting.

For the `select` field, an additional `choices` field defines the possible choices, with both labels and values:
```json
      {
        "default": "any",
        "label": "References",
        "name": "references",
        "type": "select",
        "choices": [
          {
            "value": "any",
            "name": "Any statement"
          },
          {
            "value": "referenced",
            "name": "At least one reference"
          },
          {
            "value": "no_wiki",
            "name": "At least one non-wiki reference"
          }
        ],
        "help_text": "Filter statements by their references"
      }
```
When querying the service for rows, the client can pass an optional `settings` object in each of the requested columns:
```json
     {
        "id": "P342",
        "settings": {
           "limit": "20",
           "references": "referenced",
        }
     }
```
Each key of the settings object must correspond to one form field proposed by the service. The value of that key is the value of the form field represented as a string (for uniformity and consistency with JSON form serialization).
The settings are intended to modify the results returned by the service: of course, the semantics of the settings is up to the service (as the service defines itself what settings it accepts).
