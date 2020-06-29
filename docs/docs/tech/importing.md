---
id: importing
title: Importing Architecture
sidebar_label: Importing Architecture
---

OpenRefine has a sophisticated architecture for accommodating a diverse and extensible set of importable file formats and work flows. The formats range from simple CSV, TSV to fixed-width fields to line-based records to hierarchical XML and JSON. The work flows allow the user to preview and tweak many different import settings before creating the project. In some cases, such as XML and JSON, the user also has to select which elements in the data file to import. Additionally, a data file can also be an archive file (e.g., .zip) that contains many files inside; the user can select which of those files to import. Finally, extensions to OpenRefine can inject functionalities into any part of this architecture.

## The Index Page and Action Areas

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

## The Create Project Action Area

The Create Project action area is itself extensible. Initially, it embeds a set of finger tabs corresponding to a variety of "source selection UIs": you can select a source of data by specifying a file on your computer, or you can specify the URL to a publicly accessible data file or data feed, or you can paste in from the clipboard a chunk of data.

There are actually 3 points of extension in the Create Project action area, and the first is invisible.

### Importing Controllers

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

### Data Source Selection UIs

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

### File Selection Panel
Documentation not currently available

### Parsing UI Panel
Documentation not currently available

## Server-Side Components

### ImportingController
Documentation not currently available

### UrlRewriter
Documentation not currently available

### FormatGuesser
Documentation not currently available

### ImportingParser
Documentation not currently available