---
id: quality-assurance
title: Quality assurance for Wikibase uploads
sidebar_label: Quality assurance
---

This page explains how the Wikidata extension of OpenRefine analyzes edits before they are uploaded to the Wikibase
instance. Most of these checks rely on the use of the [Wikibase Quality Constraints](https://gerrit.wikimedia.org/g/mediawiki/extensions/WikibaseQualityConstraints) extension and the configuration of the property and item identifiers in the [Wikibase manifest](./configuration).

## Overview {#overview}

Changes are scrutinized before they are uploaded, but also before the current content of the corresponding items is retrieved and merged with the updates. This means that some constraint violations cannot be predicted by the software (for instance, adding a new statement that conflicts with an existing statement on the item). However, this makes it possible to run the checks quickly, even for relatively large batches of edits. Issues are therefore refreshed in real time while the user builds the schema.

As a consequence, not all constraint violations can be detected: the ones that are supported are listed in the [Constraint violations](#constraint-violations) section. Conversely, not all issues reported will be flagged as constraint violations on the Wikibase site: see [Generic issues](#generic-issues) for these.

## Reconciliation {#reconciliation}

You should always assess the quality of your reconciliation results first. OpenRefine has various tools for quality assurance of reconciliation results. For instance:

* you can analyze the string similarity between your original names and those of the reconciled items (for instance with <span class="menuItems">Reconcile</span> → <span class="menuItems">Facets</span> → <span class="menuItems">Best candidate's name edit distance</span>);
* you can compare the values in your table with those on the items (via a text facet defined by a custom expression);
* you can facet by type on the reconciled items (add a new column with the types and use a text facet ordered by counts to get a sense of the distribution of types in your reconciled items).

## Constraint violations {#constraint-violations}

Constraints are retrieved as defined on the properties, using [ (P2302)](https://www.wikidata.org/wiki/Property:P2302).

The following constraints are supported:
* [format constraint (Q21502404)](https://www.wikidata.org/wiki/Q21502404), checked on all values
* [inverse constraint (Q21510855)](https://www.wikidata.org/wiki/Q21510855): OpenRefine assumes that the inverses of the candidate statements are not in Wikidata yet. If you know that the inverse statements are already in Wikidata, you can safely ignore this issue.
* [used for values only constraint (Q21528958)](https://www.wikidata.org/wiki/Q21528958), [used as qualifier constraint (Q21510863)](https://www.wikidata.org/wiki/Q21510863) and [used as reference constraint (Q21528959)](https://www.wikidata.org/wiki/Q21528959)
* [allowed qualifiers constraint (Q21510851)](https://www.wikidata.org/wiki/Q21510851)
* [required qualifier constraint (Q21510856)](https://www.wikidata.org/wiki/Q21510856)
* [single-value constraint (Q19474404)](https://www.wikidata.org/wiki/Q19474404): this will only trigger if you are adding more than one statement with the property on the same item, but will not detect any existing statement with this property.
* [distinct values constraint (Q21502410)](https://www.wikidata.org/wiki/Q21502410): similarly, this only checks for conflicts inside your edit batch.

A comparison of the supported constraints with respect to other implementations is available [here](https://www.wikidata.org/wiki/Wikidata:WikiProject_property_constraints/reports/implementations).

## Generic issues {#generic-issues}

OpenRefine also detects issues that are not flagged (yet) by constraint violations on Wikidata:
* Statements without references. This does not rely on [citation needed constraint (Q54554025)](https://www.wikidata.org/wiki/Q54554025): all statements are expected to have references. (The idea is that when importing a dataset, every statement you add
* should link to this dataset - it does not hurt to do it even for generic properties such as [instance of (P31)](https://www.wikidata.org/wiki/Property:P31).)
* Spurious whitespace and non-printable characters in strings (including labels, descriptions and aliases);
* Self-referential statements (statements which mention the item they belong to);
* New items created without any label;
* New items created without any description;
* New items created without any [instance of (P31)](https://www.wikidata.org/wiki/Property:P31) or [subclass of (P279)](https://www.wikidata.org/wiki/Property:P279) statement.
