# Assignment 2 — UML, parts 1 & 2

Source diagram: `OpenRefine.drawio` (open in draw.io / diagrams.net).

## Part 1 — full-system UML class diagram

A class diagram for the entire OpenRefine system. Class declarations and inheritance / implementation relations were extracted from the source tree and laid out as a single diagram. No classes are highlighted in this part — the deliverable is just the structural picture of the whole system.

## Part 2 — two essential features highlighted

Two features were chosen as the most essential candidates for someone else to extend. Each class that participates in a feature is shaded directly on the diagram with the feature's color.

| Color | Hex | Feature |
| --- | --- | --- |
| light blue | `#87CEFA` | Feature 1 — Clustering and Edit |
| light yellow | `#FFF2CC` | Feature 2 — Create Project / Project Management |

A legend with the same mapping is rendered on the diagram itself.

### Feature 1 — Clustering and Edit (light blue, `#87CEFA`)

The "cluster and edit" workflow groups similar cell values so the user can mass-edit them in one step. The shaded classes cover the full path from request, to clustering algorithm, to applying the chosen edits.

- **Clustering algorithms** — `Clusterer` (abstract), with `BinningClusterer` and `kNNClusterer` as the two strategies. `ClustererConfig` carries the request parameters and `ClusteredEntry` represents a single cell value inside a cluster.
- **Binning keyers** — `Keyer` + `KeyerFactory` register key functions. Concrete keyers: `Fingerprint`, `FingerprintKeyer`, `NGramFingerprintKeyer`, `BeiderMorseKeyer`, `ColognePhoneticKeyer`, `DaitchMokotoffKeyer`, `Metaphone3`, `Metaphone3Keyer`, `UserDefinedKeyer`.
- **kNN distance functions** — `SimilarityDistance` + `DistanceFactory` register distances. Concrete distances: `ApacheLevenshteinDistance`, `VicinoDistance`, `UserDefinedDistance`.
- **HTTP commands** — `GetClusteringFunctionsAndDistancesCommand` lists what is available; `ComputeClustersCommand` runs clustering and returns the clusters to the UI.
- **Edit application** — once the user picks a canonical value, the change is applied through `EditOneCellCommand` for a single cell, or `MassEditCommand` → `MassEditOperation` → `MassCellChange` for many cells at once.

Full list of classes shaded for Feature 1 (27 total): `Clusterer`, `ClustererConfig`, `ClusteredEntry`, `BinningClusterer`, `kNNClusterer`, `Keyer`, `KeyerFactory`, `Fingerprint`, `FingerprintKeyer`, `NGramFingerprintKeyer`, `BeiderMorseKeyer`, `ColognePhoneticKeyer`, `DaitchMokotoffKeyer`, `Metaphone3`, `Metaphone3Keyer`, `UserDefinedKeyer`, `SimilarityDistance`, `DistanceFactory`, `ApacheLevenshteinDistance`, `VicinoDistance`, `UserDefinedDistance`, `GetClusteringFunctionsAndDistancesCommand`, `ComputeClustersCommand`, `EditOneCellCommand`, `MassEditCommand`, `MassEditOperation`, `MassCellChange`.

### Feature 2 — Create Project / Project Management (light yellow, `#FFF2CC`)

How a new OpenRefine project is created from imported data and how it is represented in memory afterwards.

- **In-memory project model** — `Project` is the in-memory project, which owns the tabular data via `ColumnModel`, `Row`, and `Cell`. `ProjectMetadata` holds title / tags / modification time. `ProjectManager` is the abstract registry of all loaded projects.
- **Project creation entry point** — `CreateProjectCommand` is the HTTP command the UI calls to turn an importing job into a new `Project`.
- **Importing pipeline** — `ImportingManager` plus `ImportingController` orchestrate the upload + parsing flow that feeds `CreateProjectCommand`. Concrete format parsers shaded here: `ExcelImporter`, `JsonImporter`, `XmlImporter`.

Full list of classes shaded for Feature 2 (12 total): `Project`, `ProjectMetadata`, `ProjectManager`, `ColumnModel`, `Row`, `Cell`, `CreateProjectCommand`, `ImportingController`, `ImportingManager`, `ExcelImporter`, `JsonImporter`, `XmlImporter`.

## Part 3 — per-feature understanding packets

For each of the two highlighted features, a packet has been written so a developer who is new to the codebase can use it as a starting point for changing that feature. Each packet contains:

- a description of what the feature does end-to-end,
- a tour of the corresponding light-blue / light-yellow region of the UML diagram,
- file paths in the source tree,
- a sequence diagram of the request flow,
- recipes for the most likely kinds of changes (e.g. add a new clusterer / keyer / distance, add a new import format),
- a list of common pitfalls.

| Packet | File |
| --- | --- |
| Feature 1 — Clustering and Edit | [`feature1_clustering_and_edit.md`](./feature1_clustering_and_edit.md) |
| Feature 2 — Create Project / Project Management | [`feature2_create_project.md`](./feature2_create_project.md) |

To submit as PDFs (the assignment requires `.pdf`), open each `.md` in VS Code's Markdown preview and "Print → Save as PDF", or run a Markdown-to-PDF converter of your choice.

## Reading the diagram

Open `OpenRefine.drawio` in draw.io / diagrams.net (web or desktop). The legend block sits above the upper-left edge of the class layout. To jump to a feature's classes in the XML, search the file for its hex code (`#87CEFA` or `#FFF2CC`).
