# Wikibase extension

This extension provides the Wikibase schema editor and the Wikibase editing
backend.

## Special statement values

Wikibase supports statements whose value is explicitly unknown (`somevalue`)
or explicitly has no value (`novalue`). In an OpenRefine project, these values
can be supplied from a column used by a schema expression:

| Cell value | Wikibase snak |
| --- | --- |
| `#SOMEVALUE#` | `somevalue` |
| `#NOVALUE#` | `novalue` |

The tokens are case-sensitive and must match exactly. They can be used for a
main statement value, qualifier, or reference value wherever the schema draws
the value from a column. For example, create or transform a column so that a
row contains `#NOVALUE#`, then drag that column into the corresponding value
field in the Wikibase schema editor.

These tokens are interpreted only when evaluating a variable expression. A
cell containing ordinary text with a similar meaning (for example, `no value`)
remains an ordinary value and is not converted to a special Wikibase snak.
