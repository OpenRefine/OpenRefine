---
id: architecture
title: Architecture
sidebar_label: Architecture
---

OpenRefine is a web application, but is designed to be run locally on your own machine. The server-side maintains states of the data (undo/redo history, long-running processes, etc.) while the client-side maintains states of the user interface (facets and their selections, view pagination, etc.). The client-side makes GET and POST ajax calls to cause changes to the data and to fetch data and data-related states from the server-side.

This architecture provides a good separation of concerns (data vs. UI); allows the use of familiar web technologies (HTML, CSS, Javascript) to implement user interface features; and enables the server side to be called by third-party software through standard GET and POST operations.

## Technology stack {#technology-stack}

The server-side part of OpenRefine is implemented in Java as one single servlet which is executed by the [Jetty](http://jetty.codehaus.org/jetty/) web server + servlet container. The use of Java strikes a balance between performance and portability across operating systems (there is very little OS-specific code and has mostly to do with starting the application).

OpenRefine has no database. It uses its own in-memory data-store that is built up-front to be optimized for the operations required by faceted browsing and infinite undo.

The client-side part of OpenRefine is implemented in HTML, CSS and Javascript and uses the following libraries:
* [jQuery](http://jquery.com/)
* [jQueryUI](http:jqueryui.com/)
* [Recurser jquery-i18n](https://github.com/recurser/jquery-i18n)

The functional extensibility of OpenRefine is provided by a fork of the [SIMILE Butterfly](https://github.com/OpenRefine/simile-butterfly) modular web application framework.

Several projects provide the functionality to read and write custom format files (POI, opencsv, JENA, marc4j).

String clustering is provided by the [SIMILE Vicino](http://code.google.com/p/simile-vicino/) project.

OAuth functionality is provided by the [Signpost](https://github.com/mttkay/signpost) project.

## Server-side architecture {#server-side-architecture}

OpenRefine's server-side is written entirely in Java (`main/src/`) and its entry point is the Java servlet `com.google.refine.RefineServlet`. By default, the servlet is hosted in the lightweight Jetty web server instantiated by `server/src/com.google.refine.Refine`. Note that the server class itself is under `server/src/`, not `main/src/`; this separation leaves the possibility of hosting `RefineServlet` in a different servlet container.

The web server configuration is in `main/webapp/WEB-INF/web.xml`; that's where `RefineServlet` is hooked up. `RefineServlet` itself is simple: it just reacts to requests from the client-side by routing them to the right `Command` class in the packages `com.google.refine.commands.**`.

As mentioned before, the server-side maintains states of the data, and the primary class involved is `com.google.refine.ProjectManager`.

### Projects {#projects}

In OpenRefine there's the concept of a workspace similar to that in Eclipse. When you run OpenRefine it manages projects within a single workspace, and the workspace is embodied in a file directory with sub-directories. The default workspace directories are listed in the [FAQs](https://github.com/OpenRefine/OpenRefine/wiki/FAQ-Where-Is-Data-Stored). You can get OpenRefine to use a different directory by specifying a -d parameter at the command line.

The class `ProjectManager` is what manages the workspace. It keeps in memory the metadata of every project (in the class `ProjectMetadata`). This metadata includes the project's name and last modified date, and any other information necessary to present and let the user interact with the project as a whole. Only when the user decides to look at the project's data would `ProjectManager` load the project's actual data. The separation of project metadata and data is to minimize the amount of stuff loaded into memory.

A project's _actual_ data includes the columns, rows, cells, reconciliation records, and history entries.

A project is loaded into memory when it needs to be displayed or modified, and it remains in memory until 1 hour after the last time it gets modified. Periodically the project manager tries to save modified projects, and it saves as many modified projects as possible within 30 seconds.

### Data Model {#data-model}

A project's data consists of

- _raw data_: a list of rows, each row consisting of a list of cells
- _models_ on top of that raw data that give high-level presentation or interpretation of that data. This design lets the same raw data be viewed in different ways by different models, and let the models be changed without costly changes to the raw data.

#### Column Model {#column-model}

Cells in rows are not named and can only be addressed by their list position indices. So, a _column model_ is needed to give a name to each list position. The column model also stores other metadata for each column, including the type that cells in the column have been reconciled to and the overall reconciliation statistics of those cells.

Each column also acts as a cache for data computed from the raw data related to that column.

Columns in the column model can be removed and re-ordered without changing the raw data--the cells in the rows. This makes column removal and ordering operations really quick.

##### Column Groups {#column-groups}

Consider the following data:

![Illustration of row groups in OpenRefine](https://raw.github.com/OpenRefine/OpenRefine/2.0/graphics/row-groups.png)

Although the data is in a grid, we humans can understand that it is a tree. First of all, all rows contain data ultimately linked to the movie Austin Powers, although only one row contains the text "Austin Powers" in the "movie title" column. We also know that "USA" and "Germany" are not related to Elizabeth Hurley and Mike Myers respectively (say, as their nationality), but rather, "USA" and "Germany" are related to the movie (where it was released). We know that Mike Myers played both the character "Austin Powers" and the character "Dr. Evil"; and for the latter he received 2 awards. We humans can understand how to interpret the grid as a tree based on its visual layout as well as some knowledge we have about the movie domain but is not encoded in the table.

OpenRefine can capture our knowledge of this transformation from grid to tree using _column groups_, also stored in the column model. Each column group illustrated as a blue bracket above specifies which columns are grouped together, as well as which of those columns is the key column in that group (blue triangle). One column group can span over columns grouped by another column group, and in this way, column groups form a hierarchy determined by which column group envelopes another. This hierarchy of column groups allows the 2-dimensional (grid-shaped) table of rows and cells to be interpreted as a list of hierarchical (tree-shaped) data records.

Blank cells play a very important role. The blank cell in a key column of a row (e.g., cell "character" on row 4) makes that row (row 4) _depend_ on the first preceding row with that column filled in (row 3). This means that "Best Comedy Perf" on row 4 applies to "Dr. Evil" on row 3. Row 3 is said to be a _context row_ for row 4. Similarly, since rows 2 - 6 all have blank cells in the first column, they all depend on row 1, and all their data ultimately applies to the movie Austin Powers. Row 1 depends on no other row and is said to be a _record row_. Rows 1 - 6 together form one _record_.

Currently (as of 12th December 2017) only the XML and JSON importers create column groups, and while the data table view does display column groups but it doesn't support modifying them.

### Changes, History, Processes, and Operations {#changes-history-processes-and-operations}

All changes to the project's data are tracked (N.B. this does not include changes to a project's metadata - such as the project name.)

Changes are stored as `com.google.refine.history.Change` objects. `com.google.refine.history.Change` is an interface, and implementing classes are in `com.google.refine.model.changes.**`. Each change object stores enough data to modify the project's data when its `apply()` method is called, and enough data to revert its effect when its `revert()` method is called. It's only supposed to _store_ data, not to actually _compute_ the change. In this way, it's like a .diff patch file for a code base.

Some change objects can be huge, as huge as the project itself. So change objects are not kept in memory except when they are to be applied or reverted. However, since we still need to show the user some information about changes (as displayed in the History panel in the UI), we keep metadata of changes separate from the change objects. For each change object there is one corresponding `com.google.refine.history.HistoryEntry` for storing its metadata, such as the change's human-friendly description and timestamp.

Each project has a `com.google.refine.history.History` object that contains an ordered list of all `HistoryEntry` objects storing metadata for all changes that have been done since after the project was created. Actually, there are 2 ordered lists: one for done changes that can be reverted (undone), an done for undone changes that can be re-applied (redone). Changes must be done or redone in their exact orders in these lists because each change makes certain assumptions about the state of the project before and after it is applied. As changes cannot be undone/redone out of order, when one change fails to revert, it blocks the whole history from being reverted to any state preceding that change (as happened in [Issue #2](https://github.com/OpenRefine/OpenRefine/issues/2)).

As mentioned before, a change contains only the diff and does not actually compute that diff. The computation is performed by a `com.google.refine.process.Process` object--every change object is created by a process object. A process can be immediate, producing its change object synchronously within a very short period of time (e.g., starring one row); or a process can be long-running, producing its change object after a long time and a lot of computation, including network calls (e.g., reconciling a column).

As the user interacts with the UI on the client-side, their interactions trigger ajax calls to the server-side. Some calls are meant to modify the project. Those are handled by commands that instantiates processes. Processes are queued in a first-in-first-out basis. The first-in process gets run and until it is done all the other processes are stuck in the queue.

A process can effect a change in one thing in the project (e.g., edit one particular cell, star one particular row), or a process can effect changes in _potentially_ many things in the project (e.g., edit zero or more cells sharing the same content, starring all rows filtered by some facets). The latter kind of process is generalizable: it is meaningful to apply them on another similar project. Such a process is associated with an _abstract operation_ `com.google.refine.model.AbstractOperation` that encodes the information necessary to create another instance of that process, but potentially for a different project. When you click "extract" in the History panel, these abstract operations are called to serialize their information to JSON; and when you click "apply" in the History panel, the JSON you paste in is used to re-construct these abstract operations, which in turn create processes, which get run sequentially in a queue to generate change object and history entry pairs.

In summary,

- change objects store diffs
- history entries store metadata of change objects
- processes compute diffs and create change object and history entry pairs
- some processes are long-running and some are immediate; processes are run sequentially in a queue
- generalizable processes can be re-constructed from abstract operations

## Client-side architecture {#client-side-architecture}
The client-side part of OpenRefine is implemented in HTML, CSS and Javascript and uses the following Javascript libraries:
* [jQuery](http://jquery.com/)
* [jQueryUI](http:jqueryui.com/)
* [Recurser jquery-i18n](https://github.com/recurser/jquery-i18n)

### Importing architecture {#importing-architecture}

OpenRefine has a sophisticated architecture for accommodating a diverse and extensible set of importable file formats and work flows. The formats range from simple CSV, TSV to fixed-width fields to line-based records to hierarchical XML and JSON. The work flows allow the user to preview and tweak many different import settings before creating the project. In some cases, such as XML and JSON, the user also has to select which elements in the data file to import. Additionally, a data file can also be an archive file (e.g., .zip) that contains many files inside; the user can select which of those files to import. Finally, extensions to OpenRefine can inject functionalities into any part of this architecture.

### The Index Page and Action Areas {#the-index-page-and-action-areas}

The opening screen of OpenRefine is implemented by the file refine/main/webapp/modules/core/index.vt and will be referred to here as the index page. Its default implementation contains 3 finger tabs labeled Create Project, Open Project, and Import Project. Each tab selects an "action area". The 3 default action areas are for, obviously, creating a new project, opening an existing project, and importing a project .tar file.

Extensions can add more action areas in Javascript. For example, this is how the Create Project action area is added (refine/main/webapp/modules/core/scripts/index/create-project-ui.js):

```javascript
Refine.actionAreas.push({
  id: "create-project",
  label: "Create Project",
  uiClass: Refine.CreateProjectUI
});
```

The UI class is a constructor function that takes one argument, a jQuery-wrapped HTML element where the tab body of the action area should be rendered.

If your extension requires a very unique importing work flow, or a very novel feature that should be exposed on the index page, then add a new action area. Otherwise, try to use the existing work flows as much as possible.

### The Create Project Action Area {#the-create-project-action-area}

The Create Project action area is itself extensible. Initially, it embeds a set of finger tabs corresponding to a variety of "source selection UIs": you can select a source of data by specifying a file on your computer, or you can specify the URL to a publicly accessible data file or data feed, or you can paste in from the clipboard a chunk of data.

There are actually 3 points of extension in the Create Project action area, and the first is invisible.

#### Importing Controllers {#importing-controllers}

The Create Project action area manages a list of "importing controllers". Each controller follows a particular work flow (in UI terms, think "wizard"). Refine comes with a "default importing controller" (refine/main/webapp/modules/core/scripts/index/default-importing-controller/controller.js) and its work flow assumes that the data can be retrieved and cached in whole before getting processed in order to generate a preview for the user to inspect. (If the data cannot be retrieved and cached in whole before previewing, then another importing controller is needed.)

An importing controller is just programming logic, but it can manifest itself visually by registering one or more data source UIs and one or more custom panels in the Create Project action area. The default importing controller registers 3 such custom panels, which act like pages of a wizard.

An extension can register any number of importing controller. Each controller has a client-side part and a server-side part. Its client-side part is just a constructor function that takes an object representing the Create Project action area (usually named `createProjectUI`). The controller (client-side) is expected to use that object to register data source UIs and/or create custom panels. The controller is not expected to have any particular interface method. The default importing controller's client-side code looks like this (refine/main/webapp/modules/core/scripts/index/default-importing-controller/controller.js):

```javascript
Refine.DefaultImportingController = function(createProjectUI) {
  this._createProjectUI = createProjectUI; // save a reference to the create project action area

  this._progressPanel = createProjectUI.addCustomPanel(); // create a custom panel
  this._progressPanel.html('...'); // render the custom panel
  ... do other stuff ...
};
Refine.CreateProjectUI.controllers.push(Refine.DefaultImportingController); // register the controller
```

We will cover the server-side code below.

#### Data Source Selection UIs {#data-source-selection-uis}

Data source selection UIs are another point of extensibility in the Create Project action area. As mentioned previously, by default there are 3 data source UIs. Those are added by the default importing controller.

Extensions can also add their own data source UIs. A data source selection UI object can be registered like so

```javascript
createProjectUI.addSourceSelectionUI({
  label: "This Computer",
  id: "local-computer-source",
  ui: theDataSourceSelectionUIObject
});
```

`theDataSourceSelectionUIObject` is an object that has the following member methods:

- `attachUI(bodyDiv)`
- `focus()`

If you want to install a data source selection UI that is managed by the default importing controller, then register its UI class with the default importing controller, like so (refine/main/webapp/modules/core/scripts/index/default-importing-sources/sources.js):

```javascript
Refine.DefaultImportingController.sources.push({
    "label": "This Computer",
    "id": "upload",
    "uiClass": ThisComputerImportingSourceUI
});
```

The default importing controller will assume that the `uiClass` field is a constructor function and call it with one argument--the controller object itself. That constructor function should save the controller object for later use. More specifically, for data source UIs that use the default importing controller, they can call the controller to kickstart the process that retrieves and caches the data to import:

```javascript
controller.startImportJob(form, "... status message ...");
```

The argument `form` is a jQuery-wrapped FORM element that will get submitted to the server side at the command /command/core/create-importing-job. That command and the default importing controller will take care of uploading or downloading the data, caching it, updating the client side's progress display, and then showing the next importing step when the data is fully cached.

See refine/main/webapp/modules/core/scripts/index/default-importing-sources/sources.js for examples of such source selection UIs. While we write about source selection UIs managed by the default importing controller here, chances are your own extension will not be adding such a new source selection UI. Your extension probably adds with a new importing controller as well as a new source selection UI that work together.

#### File Selection Panel {#file-selection-panel}
Documentation not currently available

#### Parsing UI Panel {#parsing-ui-panel}
Documentation not currently available

### Server-side Components {#server-side-components}

#### ImportingController {#importingcontroller}
Documentation not currently available

#### UrlRewriter {#urlrewriter}
Documentation not currently available

#### FormatGuesser {#formatguesser}
Documentation not currently available

#### ImportingParser {#importingparser}
Documentation not currently available


## Faceted browsing architecture {#faceted-browsing-architecture}

Faceted browsing support is core to OpenRefine as it is the primary and only mechanism for filtering to a subset of rows on which to do something _en masse_ (ie in bulk). Without faceted browsing or an equivalent querying/browsing mechanism, you can only change one thing at a time (one cell or row) or else change everything all at once; both kinds of editing are practically useless when dealing with large data sets.

In OpenRefine, different components of the code need to know which rows to process from the faceted browsing state (how the facets are constrained). For example, when the user applies some facet selections and then exports the data, the exporter serializes only the matching rows, not all rows in the project. Thus, faceted browsing isn't only hooked up to the data view for displaying data to the user, but it is also hooked up to almost all other parts of the system.

### Engine Configuration {#engine-configuration}

As OpenRefine is a web app, there might be several browser windows opened on the same project, each in a different faceted browsing state. It is best to maintain the faceted browsing state in each browser window while keeping the server side completely stateless with regard to faceted browsing. Whenever the client-side needs something done by the server, it transfers the entire faceted browsing state over to the server-side. The faceted browsing state behaves much like the `WHERE` clause in a SQL query, telling the server-side how to select the rows to process.

In fact, it is best to think of the faceted browsing state as just a database query much like a SQL query. It can be passed around the whole system, to any component needing to know which rows to process. It is serialized into JSON to pass between the client-side and the server side, or to save in an abstract operation's specification. The job of the faceted browsing subsystem on the client-side is to let the user interactively modify this "faceted browsing query", and the job of the faceted browsing subsystem on the server side is to resolve that query.

In the code, the faceted browsing state, or faceted browsing query, is actually called the *engine configuration* or *engine config* for short. It consists mostly of an array facet configurations. For each facet, it stores the name of the column on which the facet is based (or an empty string if there is no base column). Each type of facet has different configuration. Text search facets have queries and flags for case-sensitivity mode and regular expression mode. Text facets (aka list facets) and numeric range facets have expressions. Each list facet also has an array of selected choices, an invert flag, and flags for whether blank and error cells are selected. Each numeric range facet has, among other things, a "from" and a "to" values. If you trace the AJAX calls, you'd see the engine configs being shuttled, e.g.,

```json
{
    "facets" : [
      {
        "type": "text",
        "name": "Shrt_Desc",
        "columnName": "Shrt_Desc",
        "mode": "text",
        "caseSensitive": false,
        "query": "cheese"
      },
      {
        "type": "list",
        "name": "Shrt_Desc",
        "columnName": "Shrt_Desc",
        "expression": "grel:value.toLowercase().split(\",\")",
        "omitBlank": false,
        "omitError": false,
        "selection": [],
        "selectBlank":false,
        "selectError":false,
        "invert":false
      },
      {
        "type": "range",
        "name": "Water",
        "expression": "value",
        "columnName": "Water",
        "selectNumeric": true,
        "selectNonNumeric": true,
        "selectBlank": true,
        "selectError": true,
        "from": 0,
        "to": 53
      }
    ],
    "includeDependent": false
  }
```

### Server-Side Subsystem {#server-side-subsystem}

From an engine configuration like the one above, the server-side faceted browsing subsystem is capable of producing:

- an iteration over the rows matching the facets' constraints
- information on how to render the facets (e.g., choice and count pairs for a list facet, histogram for a numeric range facet)

When the engine config JSON arrives in an HTTP request on the server-side, a `com.google.refine.browsing.Engine` object is constructed and initialized with that JSON. It in turns constructs zero or more `com.google.refine.browsing.facets.Facet` objects. Then for each facet, the engine calls its `getRowFilter()` method, which returns `null` if the facet isn't constrained in anyway, or a `com.google.refine.browsing.filters.RowFilter` object. Then, to when iterating over a project's rows, the engine calls on all row filters' `filterRow()` method. If and only if all row filters return `true` the row is considered to match the facets' constraints. How each row filter works depends on the corresponding type of facet.

To produce information on how to render a particular facet in the UI, the engine follows the same procedure described in the previous except it skips over the facet in question. In other words, it produces an iteration over all rows constrained by the other facets. Then it feeds that iteration to the facet in question by calling the facet's `computeChoices()` method. This gives the method a chance to compute the rendering information for its UI counterpart on the client-side. When all facets have been given a chance to compute their rendering information, the engine calls all facets to serialize their information as JSON and returns the JSON to the client-side. Only one HTTP call is needed to compute all facets.

### Client-side subsystem {#client-side-subsystem}

On the client-side there is also an engine object (implemented in Javascript rather than Java) and zero or more facet objects (also in Javascript, obviously). The engine is responsible for distributing the rendering information computed on the server-side to the right facets, and when the user interacts with a facet, the facet tells the engine to update the whole UI. To do so, the engine gathers the configuration of each facet and composes the whole engine config as a single JSON object. Two separate AJAX calls are made with that engine config, one to retrieve the rows to render, and one to re-compute the rendering information for the facets because changing one facet does affect all the other facets.
