---
id: transforming
title: Transforming data
sidebar_label: Overview
---

## Overview {#overview}

OpenRefine gives you powerful ways to clean, correct, codify, and extend your data. Without ever needing to type inside a single cell, you can automatically fix typos, convert things to the right format, and add structured categories from trusted sources. 

This section of ways to improve data are organized by their appearance in the menu options in OpenRefine. You can:

*   change the order of [rows](#edit-rows) or [columns](columnediting#rename-remove-and-move)
*   edit [cell contents](cellediting) within a particular column
*   [transform](transposing) rows into columns, and columns into rows
*   [split or join columns](columnediting#split-or-join)
*   [add new columns](columnediting) based on existing data, with fetching new information, or through [reconciliation](reconciling)
*   convert your rows of data into [multi-row records](exploring#rows-vs-records).

## Edit rows {#edit-rows}

Moving rows around is a permanent change to your data. 

You can [sort your data](sortview#sort) based on the values in one column, but that change is a temporary view setting. With that setting applied, you can make that new order permanent. 

![A screenshot of where to find the Sort menu with a sorting applied.](/img/sortPermanent.png)

In the project grid header, the word “Sort” will appear when a sort operation is applied. Click on it to show the dropdown menu, and select <span class="menuItems">Reorder rows permanently</span>. You will see the numbering of the rows change under the <span class="menuItems">All</span> column. 

:::info Reordering all rows
Reordering rows permanently will affect all rows in the dataset, not just those currently viewed through [facets and filters](facets). 
:::

You can undo this action using the [<span class="fieldLabels">History</span> tab](running#history-undoredo). 