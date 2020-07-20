---
id: key_value_columnize
title: Columnize by key/value columns
sidebar_label: Columnize by key/value
---

This operation can be used to reshape a table which contains *key* and *value* columns, such that the repeating contents in the key column become new column names, and the contents of the value column are spread in the new columns. This operation can be invoked from
any column menu, via **Transpose** → **Columnize by key/value columns**.

Overview
--------

Consider the following table:

| Field   | Data                  |
|---------|-----------------------|
| Name    | Galanthus nivalis     |
| Color   | White                 |
| IUCN ID | 162168                | 
| Name    | Narcissus cyclamineus |
| Color   | Yellow                |
| IUCN ID | 161899                |

In this format, each flower species is described by multiple attributes, which are spread on consecutive rows.
In this example, the "Field" column contains the keys and the "Data" column contains the values. With
this configuration, the *Columnize by key/value columns* operations transforms this table as follows:

| Name                  | Color    | IUCN ID |
|-----------------------|----------|---------|
| Galanthus nivalis     | White    | 162168  |
| Narcissus cyclamineus | Yellow   | 161899  |

Entries with multiple values in the same column
-----------------------------------------------

If an entry has multiple values for a given key, then these values will be grouped on consecutive rows,
to form a [record structure](../records_mode.md).

For instance, flower species can have multiple colors:

| Field       | Data                  |
|-------------|-----------------------|
| Name        | Galanthus nivalis     |
| **Color**   | **White**             |
| **Color**   | **Green**             |
| IUCN ID     | 162168                | 
| Name        | Narcissus cyclamineus |
| Color       | Yellow                |
| IUCN ID     | 161899                |

This table is transformed by the operation as follows:

| Name                  | Color    | IUCN ID |
|-----------------------|----------|---------|
| Galanthus nivalis     | White    | 162168  |
|                       | Green    |         |
| Narcissus cyclamineus | Yellow   | 161899  |

The first key encountered by the operation serves as the record key.
The "Green" value is attached to the "Galanthus nivalis" name because it is the latest record key encountered by the operation as it scans the table. See the [Row order](#row-order) section for more details about the influence of row order on
the results of the operation.

Notes column
------------

In addition to the key and value columns, a *notes* column can be used optionally. This can be used
to store extra metadata associated to a key/value pair.

Consider the following example:

| Field   | Data                  | Source                |
|---------|-----------------------|-----------------------|
| Name    | Galanthus nivalis     | IUCN                  |
| Color   | White                 | Contributed by Martha |
| IUCN ID | 162168                |                       |
| Name    | Narcissus cyclamineus | Legacy                |
| Color   | Yellow                | 2009 survey           |
| IUCN ID | 161899                |                       |

If the "Source" column is selected as notes column, this table is transformed to:

| Name                  | Color    | IUCN ID | Source : Name | Source : Color        |
|-----------------------|----------|---------|---------------|-----------------------|
| Galanthus nivalis     | White    | 162168  | IUCN          | Contributed by Martha |
| Narcissus cyclamineus | Yellow   | 161899  | Legacy        | 2009 survey           |

Notes columns can therefore be used to preserve provenance or other context about a particular key/value pair.

Extra columns
-------------

If the table contains extra columns, which are not used as key, value or notes columns, they can be preserved
by the operation. For this to work, they must have the same value in all old rows corresponding to a new row.

Consider for instance the following table, where the "Field" and "Data" columns are used as key and value columns
respectively, and the "Wikidata ID" column is not selected:

| Field   | Data                  | Wikidata ID |
|---------|-----------------------|-------------|
| Name    | Galanthus nivalis     | Q109995     |
| Color   | White                 | Q109995     |
| IUCN ID | 162168                | Q109995     |
| Name    | Narcissus cyclamineus | Q1727024    |
| Color   | Yellow                | Q1727024    |
| IUCN ID | 161899                | Q1727024    |

This will be transformed to

| Wikidata ID | Name                  | Color    | IUCN ID |
|-------------|-----------------------|----------|---------|
| Q109995     | Galanthus nivalis     | White    | 162168  |
| Q1727024    | Narcissus cyclamineus | Yellow   | 161899  |

If extra columns do not contain identical values for all old rows spanning an entry, this can
be fixed beforehand by using the [fill down operation](fill_down.md).

Row order
---------

In the absence of extra columns, it is important to note that the order in which 
the key/value pairs appear matters. Specifically, the operation will use the first key it encounters as the delimiter for entries:
every time it encounters this key again, it will produce a new row and add the following other key/value pairs to that row.

Consider for instance the following table:

| Field    | Data                  |
|----------|-----------------------|
| **Name** | Galanthus nivalis     |
| Color    | White                 |
| IUCN ID  | 162168                | 
| **Name** | Crinum variabile      |
| **Name** | Narcissus cyclamineus |
| Color    | Yellow                |
| IUCN ID  | 161899                |

The occurrences of the "Name" value in the "Field" column define the boundaries of the entries. Because there is
no other row between the "Crinum variabile" and the "Narcissus cyclamineus" rows, the "Color" and "IUCN ID" columns
for the "Crinum variabile" entry will be empty:

| Name                  | Color    | IUCN ID |
|-----------------------|----------|---------|
| Galanthus nivalis     | White    | 162168  |
| Crinum variabile      |          |         |
| Narcissus cyclamineus | Yellow   | 161899  |

This sensitivity to order is removed if there are extra columns: in that case, the first extra column will serve as root identifier
for the entries.

Behaviour in records mode
-------------------------

In records mode, this operation behaves just like in rows mode, except that any facets applied to it will be interpreted in records mode.
