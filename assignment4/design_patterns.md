# Design Patterns in OpenRefine

**Project:** [OpenRefine](https://github.com/OpenRefine/OpenRefine)

**Team members:** Kingson Zhang, Mouhamed Osman, Zhenyu Song, Zian Xu

Five design patterns identified in the OpenRefine codebase. Four are behavioral and one is structural. None of them are Singleton, Iterator, or Factory Method.

| # | Pattern | Category |
|---|---------|----------|
| 1 | Command          | Behavioral |
| 2 | Visitor          | Behavioral |
| 3 | Strategy         | Behavioral |
| 4 | Template Method  | Behavioral |
| 5 | Composite        | Structural |

---

## 1. Command (Behavioral)

**Where it lives**
- `modules/core/src/main/java/com/google/refine/history/Change.java` — the `Change` interface with `apply(Project)` and `revert(Project)`.
- `modules/core/src/main/java/com/google/refine/model/AbstractOperation.java` — abstract base for every user-facing operation; produces a `HistoryEntry` wrapping a `Change`.
- `modules/core/src/main/java/com/google/refine/history/History.java` — the invoker; keeps a list of past/future entries and drives undo/redo.
- Concrete commands: every class under `main/src/com/google/refine/operations/` (e.g. `ColumnRemovalOperation`, `MassEditOperation`).

**Conformance to GoF.** Very close to the textbook form. `Change` is the `Command` interface, with `apply` / `revert` instead of `execute` / `undo`. `History` is the invoker, and each concrete `Change` stores the data it needs to redo or undo itself.

**Why it is used here.** OpenRefine is built around non-destructive, fully reversible editing of tabular data. Modeling every edit as a command object lets `History` provide unlimited undo/redo, persist the operation list to disk for reproducibility, and replay an operation script on a different project — all without the rest of the system knowing what the operation actually does.

---

## 2. Visitor (Behavioral)

**Where it lives**
- `modules/core/src/main/java/com/google/refine/browsing/RowVisitor.java` — interface with `start(Project)`, `visit(Project, int, Row)`, `end(Project)`.
- `modules/core/src/main/java/com/google/refine/browsing/RecordVisitor.java` — companion interface for record-mode traversal.
- The traversal driver is `Engine` (`modules/core/src/main/java/com/google/refine/browsing/Engine.java`), which feeds filtered rows to a visitor.
- Concrete visitors are everywhere: exporters, aggregators, and the template engine. For example `main/src/com/google/refine/templating/Template.java` defines an inner class `RowWritingVisitor` that implements both `RowVisitor` and `RecordVisitor`.

**Conformance to GoF.** A pragmatic variant. The "elements" being visited (rows, records) are simple data containers without their own `accept` method, so `Engine` performs the dispatch externally. The separation between traversal logic and per-row logic — the core purpose of Visitor — is preserved.

**Why it is used here.** Almost every feature in OpenRefine eventually needs to iterate over the rows that match the current facet selection: export, fill-down, sum, fingerprint, template output. Visitor lets each of those features focus on what to do with a row while delegating *which* rows to visit to a single centralized engine that already understands facets and filters.

---

## 3. Strategy (Behavioral)

**Where it lives**
- `modules/core/src/main/java/com/google/refine/clustering/binning/Keyer.java` — abstract strategy for fingerprint-based clustering.
- Concrete strategies in the same package: `FingerprintKeyer`, `NGramFingerprintKeyer`, plus extension keyers like `BeiderMorseKeyer`, `Metaphone3Keyer`.
- A parallel hierarchy for kNN clustering: `modules/core/src/main/java/com/google/refine/clustering/knn/SimilarityDistance.java` with implementations such as `VicinoDistance`.
- The clusterer (`BinningClusterer`) takes a `Keyer` and applies it; the user picks the algorithm from the UI.

**Conformance to GoF.** Textbook Strategy. A family of interchangeable algorithms behind one abstract type, selected by the client at runtime.

**Why it is used here.** Clustering is the feature where OpenRefine most obviously needs algorithmic pluggability — different data demands different fingerprinting or distance functions, and researchers regularly contribute new ones. Strategy lets each algorithm be added as a standalone class with no changes to the clusterer or UI plumbing.

---

## 4. Template Method (Behavioral)

**Where it lives**
- `modules/core/src/main/java/com/google/refine/importers/ImportingParserBase.java` — abstract base. The `parse(...)` method (lines 91–117) is the template: iterate file records, call `parseOneFile(...)` on each, then optionally strip blank columns.
- `modules/core/src/main/java/com/google/refine/importers/TabularImportingParserBase.java` — second-level template that adds row/column scaffolding for tabular formats.
- Concrete subclasses override the hook methods: `SeparatorBasedImporter`, `LineBasedImporter`, `ExcelImporter`, `JsonImporter`, `XmlImporter`, etc.

**Conformance to GoF.** Faithful. The base class fixes the high-level algorithm (multi-file loop, cancellation check, blank-column cleanup) and leaves a single primitive operation, `parseOneFile`, for subclasses.

**Why it is used here.** OpenRefine has to ingest a long list of formats, but the surrounding workflow — encoding detection, progress reporting, cancellation, multi-file handling — is identical for all of them. Template Method puts that scaffolding in one place so a new format only has to supply the format-specific parsing logic.

---

## 5. Composite (Structural)

**Where it lives**
- `main/src/com/google/refine/templating/Fragment.java` — the component interface.
- `main/src/com/google/refine/templating/StaticFragment.java` and `DynamicFragment.java` — the leaves (literal text vs. an evaluated expression).
- `main/src/com/google/refine/templating/Template.java` — holds `List<Fragment> _fragments` (line 55) and renders them in order via its inner `RowWritingVisitor`.

**Conformance to GoF.** A lightweight version. There is only one level of composition (`Template` owns leaves; leaves do not nest), so the recursive structure that classical Composite enables is not exploited. The uniform-treatment idea, however, is exactly what the renderer relies on: it walks the list and writes each fragment without caring whether it is static or dynamic.

**Why it is used here.** The custom templating language used by the templating exporter mixes plain text with `${expression}` placeholders. Representing both kinds of pieces as a single `Fragment` type keeps the render loop in `Template.internalVisit` short and free of branching beyond a single `instanceof` check, and makes it trivial to add new fragment types later if the language grows.

---

## Summary

These five patterns mirror the major architectural concerns of OpenRefine: reversible editing (**Command**), facet-driven iteration (**Visitor**), pluggable algorithms (**Strategy**), uniform import scaffolding (**Template Method**), and a small template tree (**Composite**). Together they explain how OpenRefine stays extensible while keeping each subsystem focused.
