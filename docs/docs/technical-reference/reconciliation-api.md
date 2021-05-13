---
id: reconciliation-api
title: Reconciliation API
sidebar_label: Reconciliation API
---

_This is a technical description of the mechanisms behind the reconciliation system in OpenRefine. For usage instructions, see [Reconciliation](/manual/reconciling)._

A reconciliation service is a web service that, given some text which is a name or label for something, and optionally some additional details, returns a ranked list of potential entities matching the criteria. The candidate text does not have to match each entity's official name perfectly, and that's the whole point of reconciliation--to get from ambiguous text name to precisely identified entities. For instance, given the text "apple", a reconciliation service probably should return the fruit apple, the Apple Inc. company, and New York city (also known as the Big Apple).

Entities are identified by strong identifiers in some particular identifier space. In the same identifier space, identifiers follow the same syntax. For example, given the string "apple", a reconciliation service might return entities identified by the strings " [Q89](https://www.wikidata.org/wiki/Q89)", "[Q312](https://www.wikidata.org/wiki/Q312)", and "[Q60](https://www.wikidata.org/wiki/Q60)", in the Wikidata ID space. Each reconciliation service can only reconcile to one single identifier space, but several reconciliation services can reconcile to the same identifier space.

OpenRefine can connect to any reconciliation service which follows the [reconciliation API v0.1](https://reconciliation-api.github.io/specs/0.1/). This was formerly a specification edited by the OpenRefine project, which has now transitioned to its own
[W3C Entity Reconciliation Community Group](https://www.w3.org/community/reconciliation/).

Informally, the main function of any reconciliation service is to find good candidates in the underlying database, given the following data:

* A string, which is normally the name or title of the entity, in some language.
* Optionally, a type which can be used to narrow down the search to entities of this type. OpenRefine does not define a particular set of acceptable types: this choice is left to the reconciliation service (see the suggest API for that).
* Optionally, a list of properties and their values, which can be used to refine the search. For instance, when reconciling a database of books, the author name or the publication date are useful bits of information that can be transferred to the reconciliation service. This information will be sent to the reconciliation service if the user binds columns to properties. Again, the notion of property is not predefined in OpenRefine: its definition depends on the reconciliation service.

See [the specifications of the protocol](https://reconciliation-api.github.io/specs/0.1) for more details about the protocol. You can suggest changes on its [issues tracker](https://github.com/reconciliation-api/specs/issues) or on the [group mailing
list](https://lists.w3.org/Archives/Public/public-reconciliation/).

