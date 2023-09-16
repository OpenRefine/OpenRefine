---
id: architecture
title: Architecture of the Wikibase extension
sidebar_label: Wikibase extension
---


# Overview of the Wikibase extension

The Wikibase extension provides upload functionalities into [Wikibase](https://wikiba.se/) instances, by helping the user transform
data from a tabular format to [the data model of Wikibase entities](https://www.mediawiki.org/wiki/Wikibase/DataModel).

It does so by letting the user define a Wikibase schema, which as pattern of Wikibase edits which will be performed for each row.
This schema is stored inside project data as an `OverlayModel` (a generic mechanism OpenRefine offers to let extensions enrich
projects with application-specific data).
This schema typically makes reference to columns of the project as placeholders for values in various parts of the Wikibase data model.
Those edits are then scheduled (reordered) so that changes to a given entity are grouped together, and creation of new entities is done before they need to be refered to in other entities.
Once those edits are scheduled, they are first analyzed by a series of scrutinizers to detect any issues in them.
This collection of scrutinizers is largely based on the constraint system offered by [WikibaseQualityConstraints](https://www.mediawiki.org/wiki/Extension:WikibaseQualityConstraints), when it is used by the target Wikibase.
Should the user be confident in their edits, they can decide to upload them to the Wikibase instance. This first works by
comparing the edits to the existing data on the Wikibase instance, and merging the two using user-specified matching strategies.
This produces updates ready for consumption by [Wikidata-Toolkit](https://github.com/Wikidata/Wikidata-Toolkit), a Java library
which offers a client to the Wikibase API.
It is also possible to export one's edits to a textual format understood by the [QuickStatements](https://github.com/magnusmanske/quickstatements) tool, which can then perform the edits independently of OpenRefine.

The following graph gives an overview of the editing pipeline:
![Directed graph representing the pipeline from schema to upload into Wikibase](/img/editing-pipeline.png)

