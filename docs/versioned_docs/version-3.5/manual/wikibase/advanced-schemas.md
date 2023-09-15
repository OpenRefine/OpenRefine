Sometimes your data is not as simple as a normal table, or the sort of
statements that you want to do varies on each row. This document
explains how to work around these cases.

## Hierarchical data {#hierarchical-data}

Sometimes your source provides data in a structured format, such as XML,
JSON or RDF. OpenRefine can import these files and will convert them to
tables. These tables will reflect some of the hierarchy in the file by
means of null cells, using the [records mode](/manual/exploring#rows-vs-records).

The Wikibase extension always works in rows mode, so if we want to add
statements which reference both the artist and the song, we need to fill
the null cells with the corresponding artist. You can do this with the
**Fill down** operation (in the **Edit cells** menu for this column).
This function will copy not just cell values but also reconciliation
results.

## Conditional additions {#conditional-additions}

Sometimes you want to add a statement only in some conditions.

The workflow to achieve this looks like this:
- Use facets to select the rows where you do not want to add any
  information;
- Blank out the cells in the column that contain the information you
  want to add. If you do not want to lose this information, you can
  create a copy of the column beforehand;
- Remove your facets to see all rows again;
- Create a schema using the column you partially blanked out as
  statement value.

## Varying properties {#varying-properties}

Sometimes you wish you could use column variables for properties in your
schema. It is currently not possible, first because we do not have a
reconciliation service for properties yet, but also because allowing
varying properties in a statement would mean that these properties could
potentially have different datatypes, which would break the structure of
the schema.

If you only want to use a few properties, there is a way to go around
this problem. For instance, say you have a first column of altitudes and a
second column that indicates whether you should add it as
[operating altitude (P2254)](https://www.wikidata.org/wiki/Property:P2254) or as
[elevation above sea level (P2044)](https://www.wikidata.org/wiki/Property:P2044).

Create a text facet on the first column. Filter to keep only the
*altitude* values. Add a new column based on the second column, by
keeping the default expression (`value`) which just copies the existing
values. Then, select the *maximum operating altitude* value in the facet
and do the same. Reset the facet, you should have obtained two new columns
which partition the original column. You can now create a schema which adds
two statements, with values taken from those columns. Since blank values are
ignored, exactly one statement will be added for each item, with the desired property.

## Adapting to existing data on Wikibase {#adapting-to-existing-data-on-wikibase}

Sometimes you want to create statements only if there are no such
statements on the item yet. Here is one way to achieve this:

-   first, retrieve the existing values from Wikidata first, using the
    **Edit columns** → **Add columns from reconciled values** action;
-   second, create a *facet by null* on the newly created column that
    contains the information you want to control against;
-   select the non-null rows (value **false**);
-   clear the contents of the column where your source values are
    (**Edit cells** → **Common transformations** → **To null**).

You can now construct your schema as usual - null values will be ignored
when generating the statements.

