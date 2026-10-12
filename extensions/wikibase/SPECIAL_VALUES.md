# Wikidata & Wikibase: "No Value" and "Some / Unknown Value" Snaks in OpenRefine

In Wikidata and Wikibase, statements can have three types of values (snaks):
1. **Custom Value (`value`)**: A specific item, property, string, quantity, date, etc.
2. **No Value (`novalue`)**: Expresses that the entity definitely does not have a value for that property (e.g., *Douglas Adams* -> *cause of death* -> *no value* before 2001, or *country* -> *head of state* -> *no value* for direct democracies).
3. **Unknown / Some Value (`somevalue`)**: Expresses that the entity has a value for this property, but it is unknown or unspecified (e.g., *birth date* -> *unknown value* when the exact date is lost to history).

---

## How OpenRefine Handles Special Values

OpenRefine supports `novalue` and `somevalue` in its Wikibase extension using **special magic keywords** placed in cell values:

| Snak Type | Magic Keyword | Wikidata Equivalent |
|---|---|---|
| **No Value** | `#NOVALUE#` | `novalue` |
| **Some / Unknown Value** | `#SOMEVALUE#` | `somevalue` |

When OpenRefine evaluates a column bound in a Wikibase Schema, any cell whose text content is `#NOVALUE#` or `#SOMEVALUE#` is automatically converted into the corresponding Wikibase snak instead of an ordinary string or entity lookup.

---

## How to Use Them in OpenRefine

### 1. Assigning `#NOVALUE#` or `#SOMEVALUE#` with GREL

You can transform cell contents to `#NOVALUE#` or `#SOMEVALUE#` using GREL transforms on your project column:

* **Example 1: Map missing or specific markers to "no value"**
  ```grel
  if(value == "none" || value == "N/A", "#NOVALUE#", value)
  ```

* **Example 2: Map placeholder text to "unknown value"**
  ```grel
  if(value == "unknown" || isBlank(value), "#SOMEVALUE#", value)
  ```

* **Example 3: Conditional check based on another column**
  ```grel
  if(cells["has_spouse"].value == false, "#NOVALUE#", value)
  ```

---

### 2. Schema Alignment

1. In the **Wikibase / Wikidata Schema Alignment** tab, configure the target property (for example, `P26` *spouse* or `P569` *date of birth*).
2. Drag and drop the column containing your values (which may include `#NOVALUE#` or `#SOMEVALUE#` for certain rows) into the statement's value slot.
3. OpenRefine handles the evaluation automatically row-by-row:
   * Rows with standard reconciled entities or literals generate standard `value` snaks.
   * Rows with `#NOVALUE#` generate `novalue` snaks.
   * Rows with `#SOMEVALUE#` generate `somevalue` snaks.
   * Rows with empty/null values are skipped as usual.

---

### 3. Qualifiers and References

Magic values can also be used in **qualifier** and **reference** values:
* If a qualifier column evaluates to `#NOVALUE#` or `#SOMEVALUE#`, OpenRefine will attach a `novalue` or `somevalue` qualifier snak to that statement.
* If a reference snak column contains `#NOVALUE#` or `#SOMEVALUE#`, it creates a `novalue` or `somevalue` snak in the reference block.

---

### 4. Preview and Verification

* In the **Wikidata Preview** tab, statements with `#NOVALUE#` will display as `no value` (`wikibase-schema/no-value`).
* Statements with `#SOMEVALUE#` will display as `some value` (`wikibase-schema/some-value`).
* When exporting to **QuickStatements**, rows with `#NOVALUE#` export as `novalue` and `#SOMEVALUE#` export as `somevalue`.
* When performing direct edits via **Upload edits to Wikibase**, OpenRefine submits the corresponding snak structure via the MediaWiki / Wikibase API.
