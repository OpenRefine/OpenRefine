---
id: new-entities
title: Creating new items
sidebar_label: New items
---

OpenRefine can create new items. This page explains how they are
generated.

## Words of caution {#words-of-caution}

-   The fact that OpenRefine does not propose any item when reconciling
    a cell does not mean that the item is not present in the Wikibase instance:
    it can be missed for all sorts of reasons. Please make
    sure that you are not creating any duplicates!

-   Make sure that the items that you want to create are admissible in
    the Wikibase instance. For Wikidata, see the [notability guidelines](https://www.wikidata.org/wiki/Wikidata:Notability);

-   Deleting items generally requires special rights: if you want to revert an
    edit group that includes new items in Wikidata, you will need to ask an
    administrator to do it.

## Workflow overview {#workflow-overview}

Here is how you would typically create new items with OpenRefine:

-   Reconcile a column;
-   Mark some of its cells as new items. This will not create items yet.
    If you need to mark many rows as new items, use the **Reconcile** →
    **Actions** → **Create a new item for each cell** operation.
-   Create a Wikibase schema as usual, using the column where your new
    items are marked;
-   Perform the edits: the new items will be created on Wikidata at this
    point;
-   The cells that you had marked as new items will now be reconciled to
    the newly-created items.

It is often useful (but not mandatory) to treat new items in isolation
and use a dedicated schema for them. This helps you add many statements
on the new items (including labels and descriptions) without risking to
clutter existing items with redundant edits. Use a facet on the judgment
status of the reconciled column to isolate new items and perform their
edits separately. As always in OpenRefine, only the rows covered by your
facets will be considered when uploading the edits to Wikidata: if a
cell is reconciled to a new item but is excluded by the facet, no new
item will be created for it.[^1]

Note that even if you know that all items in your column are new, you
will still need to make a first reconciliation pass by selecting the
Wikidata reconciliation service, and then setting all reconciliation
statuses to \"new\". If you skip the first part, OpenRefine will not
know that this column is reconciled against your Wikibase instance (it could be
reconciled to other services) so it will not let you use it in place of
an item in a Wikibase schema.

You can also perform the edits with QuickStatements - in this case, your
OpenRefine project will not be updated with the newly created Qids.

## Adding labels to new items {#adding-labels-to-new-items}

The text that is in a cell reconciled to \"new\" is not automatically
used as label for the newly-created item. This is because OpenRefine has
no way to guess in which language this label should be. When adding new
items, you need to explicitly add a label in the schema. This label can
use the reconciled column as source, but if you have other cells matched
to existing items, be careful not to override the labels of these items
(if it is not your intention).

OpenRefine will refuse to perform edits where new items are created
without any labels (as this is considered a critical issue). Other
issues will be raised if insufficient basic information is added on the
items (but these other warnings will not prevent you from performing the
edits).

## Marking multiple cells as identical items {#marking-multiple-cells-as-identical-items}

If you mark individual cells as new items, one new item per cell will be
created. Sometimes multiple rows refer to the same item. OpenRefine
makes it possible to mark all the corresponding cells as the *same* new
item. Two conditions have to be met:
- the reconciled cells must be in the same column (it is not possible
  to mark two cells in different colums as the same new item);
- the cells must contain the same initial text value.

If these two conditions are met, then isolate these cells with facets
and go to **Reconcile** → **Actions** → **Create one item for similar
cells**. This will mark the cells as new and referring to the same item.

## Retrieving the Qids of the newly-created items {#retrieving-the-qids-of-the-newly-created-items}

Once you have performed your edits with OpenRefine, any new cells
covered by the facet will be updated with their new Qids. You can
retrieve these Qids with the **Edit column** → **Add column based on
this column** action and using the `cell.recon.match.id` expression.
Note that you will no longer be able to isolate new items with a
judgment facet at this stage (because the judgment will be updated to
**matched**) so it can be worth marking these rows (for instance with a
star or flag) before performing the edits.

[^1]: The only exception to this rule is when marking multiple cells as
    identical items: in this case, if one of such cells are included in
    the facet, then all the others will be updated with the newly
    created Qid once the edits are made.
