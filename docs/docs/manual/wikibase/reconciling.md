---
id: reconciling
title: Reconciling with Wikibase
sidebar_label: Reconciling with Wikibase
---

The Wikidata [reconciliation service](reconciling) for OpenRefine [supports](https://reconciliation-api.github.io/testbench/):
*   A large number of potential types to reconcile against
*   Previewing and viewing entities
*   Suggesting entities, types, and properties
*   Augmenting your project with more information pulled from Wikidata. 

You can find documentation and further resources on the reconciliation API [here](https://wikidata.reconci.link/).

For the most part, Wikidata reconciliation behaves the same way other reconciliation services do, but there are a few processes and features specific to Wikidata. 

## Language settings {#language-settings}

You can install a version of the Wikidata reconciliation service that uses your language. First, you need the language code: this is the [two-letter code found on this list](https://en.wikipedia.org/wiki/List_of_Wikipedias), or in the domain name of the desired Wikipedia/Wikidata (for instance, “fr” if your Wikipedia is https://fr.wikipedia.org/wiki/).

Then, open the reconciliation window (under <span class="menuItems">Reconcile</span> → <span class="menuItems">Start reconciling...</span>) and click <span class="menuItems">Add Standard Service</span>. The URL to enter is `https://wikidata.reconci.link/fr/api`, where “fr” is your desired language code.

When reconciling using this interface, items and properties will be displayed in your chosen language if the label is available. The matching score of the reconciliation is not influenced by your choice of language for the service: items are matched by considering all labels and returning the best possible match. The language of your dataset is also irrelevant to your choice of language for the reconciliation service; it simply determines which language labels to return based on the entity chosen.

## Restricting matches by type {#restricting-matches-by-type}

In Wikidata, types are items themselves. For instance, the [university of Ljubljana (Q1377)](https://www.wikidata.org/wiki/Q1377) has the type [public university (Q875538)](https://www.wikidata.org/wiki/Q875538), using the [instance of (P31)](https://www.wikidata.org/wiki/Property:P31) property. Types can be subclasses of other types, using the [subclass of (P279)](https://www.wikidata.org/wiki/Property:P279) property. For instance, [public university (Q875538)](https://www.wikidata.org/wiki/Q875538) is a subclass of [university (Q3918)](https://www.wikidata.org/wiki/Q3918). You can visualize these structures with the [Wikidata Graph Builder](https://angryloki.github.io/wikidata-graph-builder/). 

When you select or enter a type for reconciliation, OpenRefine will include that type and all of its subtypes. For instance, if you select [university (Q3918)](https://www.wikidata.org/wiki/Q3918), then [university of Ljubljana (Q1377)](https://www.wikidata.org/wiki/Q1377) will be a possible match, though that item isn't directly linked to Q3918 - because it is directly linked to Q875538, the subclass of Q3918.

Some items and types may not yet be set as an instance or subclass of anything (because Wikidata is crowdsourced). If you restrict reconciliation to a type, items without the chosen type will not appear in the results, except as a fallback, and will have a lower score.

## Reconciling via unique identifiers {#reconciling-via-unique-identifiers}

You can supply a column of unique identifiers (in the form "Q###" for entities) directly to Wikidata in order to pull more data, but [these strings will not be “reconciled” against the external dataset](reconciling#reconciling-with-unique-identifiers). Apply the operation <span class="menuItems">Reconcile</span> → <span class="menuItems">Use values as identifiers</span> on your column of QIDs. All cells will appear as dark blue “confirmed” matches. Some of the “matches” may be errors, which you will need to hover over or click on to identify. You cannot use this to reconcile properties (in the form "P###").

If the identifier you submit is assigned to multiple Wikidata items (because Wikidata is crowdsourced), all of the items are returned as candidates, with none automatically matched.

## Property paths, special properties, and subfields {#property-paths-special-properties-and-subfields}

Wikidata's hierarchical property structure can be called by using property paths (using |, /, and . symbols). Labels, aliases, descriptions, and sitelinks can also be accessed. You can also match values against subfields, such as latitude and longitude subfields of a geographical coordinate.

For information on how to do this, read the [documentation and further resources here](https://wikidata.reconci.link/#documentation).


