---
id: architecture
title: Architecture of the Wikibase extension
sidebar_label: Wikibase extension
---


# Overview of the Wikibase extension

The Wikibase extension provides upload functionalities into [Wikibase](https://wikiba.se/) instances, by helping the user transform
data from a tabular format to [the data model of Wikibase entities](https://www.mediawiki.org/wiki/Wikibase/DataModel).

The following graph gives an overview of the editing pipeline:
![Directed graph representing the pipeline from schema to upload into Wikibase](/img/editing-pipeline.png)

1. The schema is evaluated (`WikibaseSchema::evaluate`) on each visible row in the project. This gives rise to one or more `EntityEdit` objects for each row. Those objects represent a candidate edit, including the configuration of how this edit should be matched to any
   existing data on the entity to be edited.
2. The `WikibaseAPIScheduler` groups the edits together, so that they can later be performed with the HTTP Wikibase API efficiently. This essentially means bundling up all changes made to the same entity across the project into a single object. If new
   edits are made, those edits also need to be ordered so that any new entity referenced in an edit already exists by the time this edit is made.
3. A collection of `EditScrutinizer`s are executed on the candidate edits, to check for detectable issues about those. This generate `QAWarning` objects which are aggregated according to keys defined by the scrutinizers. This collection of scrutinizers is largely based on the constraint system offered by [WikibaseQualityConstraints](https://www.mediawiki.org/wiki/Extension:WikibaseQualityConstraints), when it is used by the target Wikibase.

The steps above are executed every time the schema is previewed. When the user is ready to upload their edits to Wikibase, they have a choice between two routes:
 
4. Going through [QuickStatements](https://github.com/magnusmanske/quickstatements). The `QuickStatementsScheduler` offers another way to group edits together, such that they can be expressed in the QuickStatements v1 format. The `QuickStatementsExporter` translates them to that format.

They can also upload their edits directly from OpenRefine, in which case the following operations are executed for each edit:

5. The edit is rewritten, so that if it references any newly created Wikibase entity, the identifier of this entity is inserted in the edit. This is done by `ReconEntityRewriter`.
6. For edits to existing entities, the edit is compared to any existing data on the item. Following the matching criteria stored in the edit, we generate a concrete update of entity data. This is done by `EntityEdit::toUpdate`.
7. Finally, the update is sent to the Wikibase instance using [Wikidata-Toolkit](https://github.com/Wikidata/Wikidata-Toolkit).

