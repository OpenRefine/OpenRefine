# Feature 2 Packet — Create Project / Project Management

> Audience: a developer who has never touched OpenRefine before, but is being asked to extend or change how projects are created and managed (e.g. add a new import format, change how project metadata is stored, change the create-project request shape).

This packet is the companion to the Feature 2 highlights (light yellow, `#FFF2CC`) in `OpenRefine.drawio`. Read it alongside the diagram.

---

## 1. What the feature does

A **project** in OpenRefine is one tabular dataset the user is working on, plus its history of edits. Project management is everything around a project's lifecycle:

1. **Create** — user uploads / pastes / points at a data source; the server parses it and registers a new `Project`.
2. **Live** — the project sits in memory under `ProjectManager`, indexed by a `long` id, with a `ProjectMetadata` record (title, tags, modified time).
3. **Persist / list / rename / delete / export / import** — the user comes back later and sees the project in the project list, opens it, exports to disk, or removes it.

The shaded part of the diagram focuses on **create** plus the in-memory model that everything else operates on.

## 2. Where it lives in the UML diagram

All of the classes named in this packet are filled with **light yellow (`#FFF2CC`)** in `OpenRefine.drawio`. There are 12 of them:

| Sub-area | Classes |
| --- | --- |
| In-memory project model | `Project`, `ProjectMetadata`, `ColumnModel`, `Row`, `Cell` |
| Project registry | `ProjectManager` |
| Create entry point (HTTP) | `CreateProjectCommand` |
| Importing pipeline | `ImportingController`, `ImportingManager` |
| Concrete format parsers | `ExcelImporter`, `JsonImporter`, `XmlImporter` |

## 3. Where it lives in the source tree

| Path | What's there |
| --- | --- |
| `modules/core/src/main/java/com/google/refine/model/Project.java` | `Project` |
| `modules/core/src/main/java/com/google/refine/ProjectMetadata.java` | `ProjectMetadata` |
| `modules/core/src/main/java/com/google/refine/ProjectManager.java` | `ProjectManager` (abstract registry) |
| `modules/core/src/main/java/com/google/refine/io/FileProjectManager.java` | the only production subclass — saves projects under `~/.local/share/openrefine/<id>/` (path varies by OS) |
| `modules/core/src/main/java/com/google/refine/model/ColumnModel.java`, `Row.java`, `Cell.java` | tabular data inside a `Project` |
| `main/src/com/google/refine/commands/project/CreateProjectCommand.java` | HTTP endpoint `POST /command/core/create-project-from-import-job` |
| `modules/core/src/main/java/com/google/refine/importing/ImportingManager.java` | static registry of formats → parsers, controllers, guessers |
| `modules/core/src/main/java/com/google/refine/importing/ImportingController.java` | interface; one implementation per "source kind" (default upload, database) |
| `modules/core/src/main/java/com/google/refine/importing/ImportingJob.java` | per-upload state (temp dir, progress, current options) — referenced indirectly by the diagram via `ImportingManager` |
| `modules/core/src/main/java/com/google/refine/importing/ImportingUtilities.java` | the actual `createProject(...)` method that ties parser + project together |
| `modules/core/src/main/java/com/google/refine/importers/...` | **base** parser classes |
| `main/src/com/google/refine/importers/ExcelImporter.java` etc. | concrete parsers (`ExcelImporter`, `JsonImporter`, `XmlImporter`, plus `OdsImporter`, `LineBasedImporter`, `RdfTripleImporter`, …) |

## 4. Two pictures of the in-memory model

### 4a. A `Project` is a small object tree

```
Project
 ├── ProjectMetadata (title, tags, created, modified, …)
 ├── ColumnModel
 │     └── List<Column>
 ├── List<Row>
 │     └── each Row has List<Cell>
 └── History (the redo/undo log; not in this feature's scope)
```

`ColumnModel` is the schema, `Row`+`Cell` are the data. Almost every other feature in OpenRefine reads or mutates this tree.

### 4b. Projects are looked up via a registry

```
ProjectManager.singleton  (abstract, but in production this is a FileProjectManager)
 └── _projects: Map<Long, Project>
 └── _projectsMetadata: Map<Long, ProjectMetadata>
```

`ProjectManager.registerProject(project, metadata)` is the moment a fresh project becomes addressable by its id everywhere else in the app.

## 5. The importing pipeline (registry of formats)

`ImportingManager` is a process-wide static registry. It is populated at startup by each module that knows how to read a format. Roughly:

```
ImportingManager.formatToRecord : Map<String, Format>
   "text/xml/xml"      → Format(label="XML",   parser=XmlImporter,   uiClass="XmlParserUI")
   "text/json"         → Format(label="JSON",  parser=JsonImporter,  uiClass="JsonParserUI")
   "binary/xls"        → Format(label="Excel", parser=ExcelImporter, uiClass="ExcelParserUI")
   …
ImportingManager.controllers : Map<String, ImportingController>
   "core"              → DefaultImportingController     (file/url upload)
   "database"          → DatabaseImportController       (JDBC source)
ImportingManager.formatToGuessers, extensionToFormat, mimeTypeToFormat
   – used to auto-detect the format from the user's file
```

Format detection at upload time: take the file's extension or MIME type → look up format id → run any registered `FormatGuesser`s for that format to refine the choice → present that `Format` to the user.

## 6. Sequence diagram — creating a project

The UI walks through three steps. Step 3 is what `CreateProjectCommand` handles.

```
 Browser            ImportingControllerCommand           DefaultImportingController          ImportingManager     ImportingUtilities       Project / ProjectManager
   │                          │                                       │                              │                    │                           │
   │ Step 1: create job (upload + format guess)                       │                              │                    │                           │
   │ POST /command/core/      │  controller="core"                    │                              │                    │                           │
   │   importing-controller    │   subCommand="load-raw-data"         │                              │                    │                           │
   │ ───────────────────────► │ ─────────────────────────────────────►│  pickFormat(...)             │                    │                           │
   │                          │                                       │ ────────────────────────────►│                    │                           │
   │ ◄────────────────────────                                                                                                                          │
   │ {jobID: …}               │                                       │                              │                    │                           │
   │                          │                                       │                              │                    │                           │
   │ Step 2: preview / set parser options (loop, same endpoint)        │                              │                    │                           │
   │                          │                                       │                              │                    │                           │
   │ Step 3: actually create the Project                              │                              │                    │                           │
   │ POST /command/core/      │                                       │                              │                    │                           │
   │   create-project-from-   │                                       │                              │                    │                           │
   │   import-job             │  ──────────────► CreateProjectCommand ──────────────────────────────►│                    │                           │
   │                          │                  • read jobID, format, options                       │ getFormat(format)  │                           │
   │                          │                  • ImportingUtilities.createProject(job, format, …)  │ ──────────────────►│                           │
   │                          │                                                                      │                    │ run Format.parser         │
   │                          │                                                                      │                    │ (ExcelImporter /          │
   │                          │                                                                      │                    │  JsonImporter /           │
   │                          │                                                                      │                    │  XmlImporter / …)         │
   │                          │                                                                      │                    │ to fill rows/columns      │
   │                          │                                                                      │                    │ ─────────────────────────►│
   │                          │                                                                      │                    │ ProjectManager.singleton  │
   │                          │                                                                      │                    │   .registerProject(...)   │
   │                          │                                                                      │                    │ ─────────────────────────►│
   │ ◄────────────────────────                                                                                                                          │
   │ {code:"ok", projectID}   │                                                                                                                          │
```

Key call site: `ImportingUtilities.createProject(...)` (found at `main/src/.../commands/project/CreateProjectCommand.java:129`). That single call drives:

1. instantiate the `Format`'s `ImportingParser` (e.g. `ExcelImporter`),
2. `parse(...)` against an empty `Project` to populate `ColumnModel`, `Row`, `Cell`,
3. build a `ProjectMetadata`,
4. call `ProjectManager.singleton.registerProject(project, metadata)`,
5. return the new project id.

## 7. Recipes for likely changes

### 7a. Add a new import format (e.g. a custom binary)

1. Create `MyFormatImporter` next to `ExcelImporter` / `JsonImporter` / `XmlImporter`. Pick the right base:
   - **Tabular row-by-row source** → extend `TabularImportingParserBase`.
   - **Tree source** (XML-shaped) → extend `TreeImportingParserBase`. (`XmlImporter` and `JsonImporter` follow this path.)
   - **Anything else** → extend `ImportingParserBase`.
2. Register it at module startup:
   ```java
   ImportingManager.registerFormat(
       "binary/myformat",  // format id
       "My Format",        // user-visible label
       "MyFormatParserUI", // JS class for options panel
       new MyFormatImporter());
   ImportingManager.registerExtension("mfm", "binary/myformat");
   ImportingManager.registerMimeType("application/x-myformat", "binary/myformat");
   ```
3. (Optional) register a `FormatGuesser` if the file extension/MIME alone isn't enough to identify it.
4. Add a JS module file under `main/webapp/modules/core/scripts/index/parser-interfaces/` matching the `uiClass` you registered, otherwise the user can't configure parser options.
5. After this you do **not** need to touch `CreateProjectCommand` — it's format-agnostic; it looks the format up by id at request time.

### 7b. Add a new "where data comes from" controller

If you want to ingest from somewhere that isn't the upload form (e.g. a Google Sheet, an S3 URL, a streaming queue), implement a new `ImportingController`. `DefaultImportingController` (uploads) and `DatabaseImportController` (JDBC) are the existing examples. Register with `ImportingManager.controllers.put("yoursource", new YourController())` at startup.

### 7c. Add a field to `ProjectMetadata`

1. Add the field, getter, and setter in `ProjectMetadata`.
2. Annotate it for Jackson so it round-trips through JSON.
3. The `FileProjectManager` writes `ProjectMetadata` as JSON next to each project's data files. Jackson handles forward-compatibility for new fields, but **bump nothing manually**: existing projects without the field will read back with the default.
4. If the new field needs to be settable from the UI, the relevant command is `SetProjectMetadataCommand` (not in the shaded set, but lives next to `CreateProjectCommand`). It already takes arbitrary key/value pairs.

### 7d. Change the project storage layout

`ProjectManager` is abstract; **`FileProjectManager`** is the production subclass that decides where on disk the workspace lives. Override `saveProject`, `loadProjectMetadata`, `importProject`, `exportProject` etc. To experiment with a different layout (e.g. SQLite-backed), subclass `ProjectManager` and install your subclass via `ProjectManager.singleton = new MyProjectManager(...)` at boot.

## 8. Things that are easy to miss

- **`ProjectManager.singleton` is a static.** There is exactly one project manager per OpenRefine process. Many call sites use it directly (`ProjectManager.singleton.getProject(id)`) rather than through dependency injection.
- **`CreateProjectCommand` is *not* where parsing happens.** It is a thin wrapper around `ImportingUtilities.createProject(...)`. Don't put parsing logic in the command — put it in your `ImportingParser` subclass.
- **Two staging points before a project exists.**
  1. `ImportingJob` in `ImportingManager.allJobs` — temporary upload + parser options. The job has a `dir` on disk under `<workspace>/jobs/<id>/`.
  2. The `Project` itself — only created in step 3 above; the job is then disposed.
  Bugs that look like "my project doesn't appear" usually fail in step 3 silently; check the response body of `create-project-from-import-job`.
- **Format ids, extensions, MIME types are all lowercase strings.** A typo in registration is the #1 reason a new format silently doesn't show up.
- **`Row`, `Cell`, `ColumnModel` are touched by almost every other feature.** Treat them as a public contract — adding fields is fine, removing or renaming will cascade.

## 9. Suggested next steps for someone making a change

1. Open `OpenRefine.drawio` and find the 12 light-yellow classes; the cluster of `Project / ProjectMetadata / ColumnModel / Row / Cell` is the in-memory shape, the cluster of `ImportingController / ImportingManager / *Importer` is the create pipeline.
2. Run OpenRefine locally (`./refine`) and watch the network tab in the browser while uploading a small CSV. You'll see exactly the three POSTs in §6.
3. Set a breakpoint in `CreateProjectCommand.doPost`, then step into `ImportingUtilities.createProject`. The fastest way to internalise the pipeline.
4. The lowest-risk first change is **7a (new format)**. Start by copy-pasting `JsonImporter` and renaming everything; it's a complete worked example of every registration call you need.
