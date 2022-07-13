---
id: writing-extensions
title: Writing Extensions
sidebar_label: Writing Extensions
---

## Introduction {#introduction}

This is a very brief overview of the structure of OpenRefine extensions. For more detailed documentation and step-by-step guides please see the following external documentation/tutorials:

* Giuliano Tortoreto has [written documentation detailling how to build extension for OpenRefine](https://github.com/giTorto/OpenRefineExtensionDoc/raw/master/main.pdf)
* Owen Stephens has written [a guide to developing an extension which adds new GREL functions to OpenRefine](http://www.meanboyfriend.com/overdue_ideas/2017/05/writing-an-extension-to-add-new-grel-functions-to-openrefine/).

OpenRefine makes use of a modified version of the [Butterfly framework](https://github.com/OpenRefine/simile-butterfly/tree/openrefine) to provide an extension architecture. OpenRefine extensions are Butterfly modules. You don't really need to know about Butterfly itself, but you might encounter "butterfly" here and there in the code base.

Extensions that come with the code base are located under [the extensions subdirectory](https://github.com/OpenRefine/OpenRefine/tree/master/extensions), but when you develop your own extension, you can put its code anywhere as long as you point Butterfly to it. That is done by any one of the following methods

* refer to your extension's directory in [the butterfly.properties file](https://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/WEB-INF/butterfly.properties) through a `butterfly.modules.path` setting.
* specify the butterfly.modules.path property on the command line when you run OpenRefine. This overrides the values in the property file, so you need to include the default values first e.g. `-Dbutterfly.modules.path=modules,../../extensions,/path/to/your/extension`

Please note that you should bundle any dependencies yourself, so you are insulated from OpenRefine packaging changes over time.

### Directory Layout {#directory-layout}

A OpenRefine extension sits in a file directory that contains the following files and sub-directories:

```
pom.xml
  src/
      com/foo/bar/... *.java source files
  module/
      *.html, *.vt files
      scripts/... *.js files
      styles/... *.css and *.less files
      images/... image files
      MOD-INF/
          lib/*.jar files
          classes/... java class files
          module.properties
          controller.js
```

The file named module.properties (see [example](https://github.com/OpenRefine/OpenRefine/blob/master/extensions/sample/module/MOD-INF/module.properties)) contains the extension's metadata. Of importance is the name field, which gives the extension a name that's used in many other places to refer to it. This can be different from the extension's directory name.

```
name = my-extension-name
```

Your extension's client-side resources (.html, .js, .css files) stored in the module/ subdirectory will be accessible from http://127.0.0.1:3333/extension/my-extension-name/ when OpenRefine is running.

Also of importance is the dependency

```
requires = core
```

which makes sure that the core module of OpenRefine is loaded before the extension attempts to hook into it.

The file named controller.js is responsible for registering the extension's hooks into OpenRefine. Look at the sample-extension extension's [controller.js](https://github.com/OpenRefine/OpenRefine/blob/master/extensions/sample/module/MOD-INF/controller.js) file for an example. It should have a function called init() that does the hook registrations.

The `pom.xml` file is an [Apache Maven](http://maven.apache.org/) build file. You can make a copy of the sample extension's `pom.xml` file to get started. The important point here is that the Java classes should be built into the `module/MOD-INF/classes` sub-directory.

Note that your extension's Java code would need to reference some libraries used in OpenRefine and OpenRefine's Java classes themselves. These dependencies are reflected in the Maven configuration for the extension.

## Sample extension {#sample-extension}

The sample extension is included in the code base so that you can copy it and get started on writing your own extension. After you copy it, make sure you change its name inside its `module/MOD-INF/controller.js` file.

### Basic Structure {#basic-structure}

The sample extension's code is in `refine/extensions/sample/`. In that directory, Java source code is contained under the `src` sub-directory, and webapp code is under the `module` sub-directory. Here is the full directory layout:

```
refine/extensions/sample/
      build.xml (ant build script)
      src/
          com/google/refine/sampleExtension/
              ... Java source code ...
      module/
          MOD-INF/
              module.properties (module settings)
              controller.js (module init and routing logic in Javascript)
          classes/
              ... compiled Java classes ...
          lib/
              ... Java jars ...
          ... velocity templates (.vt) ...
          ... LESS css files ...
          ... client-side files (.html, .css, .js, image files) ...
```

The sub-directory `MOD-INF` contains the Butterfly module's metadata and is what Butterfly looks for when it scans directories for modules. `MOD-INF` serves similar functions as `WEB-INF` in other web frameworks.

Java code is built into the sub-directory `classes` inside `MOD-INF`, and supporting external Java jars are in the `lib` sub-directory. Those will be automatically loaded by Butterfly. (The build.xml script is wired to compile into the `classes` sub-directory.)

Client-side code is in the inner `module` sub-directory. They can be plain old .html, .css, .js, and image files, or they can be [LESS](http://lesscss.org/) files that get processed into CSS. There are also Velocity .vt files, but they need to be routed inside `MOD-INF/controller.js`.

`MOD-INF/controller.js` lets you configure the extension's initialization and URL routing in Javascript rather than in Java. For example, when the requested URL path is either `/` or an empty string, we process and return `MOD-INF/index.vt` ( [see http://127.0.0.1:3333/extension/sample/](http://127.0.0.1:3333/extension/sample/) if OpenRefine is running).

The `init()` function in `controller.js` allows the extension to register various client-side handlers for augmenting pages served by Refine's core. These handlers are feature-specific. For example, [this is where the jython extension adds its parser](https://github.com/OpenRefine/OpenRefine/blob/master/extensions/jython/module/MOD-INF/controller.js#L46). As for the sample extension, it adds its script `project-injection.js` and style `project-injection.less` into the `/project` page. If you [view the source of the /project page](http://127.0.0.1:3333/project), you will see references to those two files.

### Wiring Up the Extension {#wiring-up-the-extension}

The Extensions are loaded by the Butterfly framework. Butterfly refers to these as 'modules'. [The location of modules is set in the `main/webapp/butterfly.properties` file](https://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/WEB-INF/butterfly.properties#L27). Butterfly simply descends into each of those paths and looks for any `MOD-INF` directories.

For more information, see [Extension Points](https://github.com/OpenRefine/OpenRefine/wiki/Extension-Points).

## Extension points {#extension-points}

### Client-side: Javascript and CSS {#client-side-javascript-and-css}

The UI in OpenRefine for working with a project is coded in [the /main/webapp/modules/core/project.vt file](http://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/project.vt). The file is quite small, and that's because almost all of its content is to be expanded dynamically through the Velocity variables $scriptInjection and $styleInjection. So that your own Javascript and CSS files get loaded, you need to register them with the ClientSideResourceManager, which is done in the /module/MOD-INF/controller.js file. See [the controller.js file in this sample extension code](http://github.com/OpenRefine/OpenRefine/blob/master/extensions/sample/module/MOD-INF/controller.js) for an example.

In the registration call, the variable `module` is already available to your code by default, and it refers to your own extension.

```
ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "scripts/foo.js",
            "scripts/subdir/bar.js"
        ]
    );
```

You can specify one or more files for registration, and their paths are relative to the `module` subdirectory of your extension. They are included in the order listed.

Javascript Bundling: Note that `project.vt` belongs to the core module and is thus under the control of the core module's [controller.js file](http://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/MOD-INF/controller.js). The Javascript files to be included in `project.vt` are by default bundled together for performance. When debugging, you can prevent this bundling behavior by setting `bundle` to `false` near the top of that `controller.js` file. (If you have commit access to this code base, be sure not to check that change in.)

### Client-side: Images {#client-side-images}

We recommend that you always refer to images through your CSS files rather than in your Javascript code. URLs to images will thus be relative to your CSS files, e.g.,

```
.foo {
    background: url(../images/x.png);
  }
```

If you really really absolutely need to refer to your images in your Javascript code, then look up your extension's URL path in the global Javascript variable `ModuleWirings`:

```
ModuleWirings["my-extension"] + "images/x.png"
```

### Client-side: HTML Templates {#client-side-html-templates}

Beside Javascript, CSS, and images, your extension might also include HTML templates that get loaded on the fly by your Javascript code and injected into the page's DOM. For example, here is [the Cluster edit dialog template](http://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/scripts/dialogs/clustering-dialog.html), which gets loaded by code in [the equivalent javascript file 'clustering-dialog.js'](http://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/scripts/dialogs/clustering-dialog.js):

```
var dialog = $(DOM.loadHTML("core", "scripts/dialogs/clustering-dialog.html"));
```

`DOM.loadHTML` returns the content of the file as a string, and `$(...)` turns it into a DOM fragment. Where `"core"` is, you would want your extension's name. The path of the HTML file is relative to your extension's `module` subdirectory.

### Client-side: Project UI Extension Points {#client-side-project-ui-extension-points}

Getting your extension's Javascript code included in `project.vt` doesn't accomplish much by itself unless your code also registers hooks into the UI. For example, you can surely implement an exporter in Javascript, but unless you add a corresponding menu command in the UI, your user can't use your exporter.

#### Main Menu {#main-menu}

The main menu can be extended by calling any one of the methods `MenuBar.appendTo`, `MenuBar.insertBefore`, and `MenuBar.insertAfter`. Each method takes 2 arguments: an array of strings that identify a particular existing menu item or submenu, and one new single menu item or submenu or an array of menu items and submenus. For example, to insert 2 menu items and a menu separator before the menu item Project > Export Filtered Rows > Templating..., write this Javascript code wherever that would execute when your Javascript files get loaded:

```
MenuBar.insertBefore(
        ["core/project", "core/export", "core/export-templating"],
        [
            {
                "label":"Menu item 1",
                "click": function() { ... }
            },
            {
                "label":"Menu item 2",
                "click": function() { ... }
            },
            {} // separator
        ]
    );
```

The array `["core/project", "core/export", "core/export-templating"]` pinpoints the reference menu item.

See the beginning of [/main/webapp/modules/core/scripts/project/menu-bar.js](http://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/scripts/project/menu-bar.js) for IDs of menu items and submenus.

#### Column Header Menu {#column-header-menu}

The drop-down menu of each column can also be extended, but the mechanism is slightly different compared to the main menu. Because the drop-down menu for a particular column is constructed on the fly when the user actually clicks the drop-down menu button, extending the column header menu can't really be done once at start-up time, but must be done every time a column header menu gets created. So, registration in this case involves providing a function that gets called each such time:

```
DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) { ... do stuff to menu ... });
```

That function takes in the column object (which contains the column's name), the column header UI object (generally not so useful), and the menu to extend. In the previous code line where it says "do stuff to menu", you can write something like this:

```
MenuSystem.appendTo(menu, ["core/facet"], [
        {
            id: "core/text-facet",
            label: "My Facet on " + column.name,
            click: function() {
                ... use column.name and do something ...
            }
        },
    ]);
```

In addition to `MenuSystem.appendTo`, you can also call `MenuSystem.insertBefore` and `MenuSystem.insertAfter` which the same 3 arguments. To see what IDs you can use, see the function `DataTableColumnHeaderUI.prototype._createMenuForColumnHeader` in [/main/webapp/modules/core/scripts/views/data-table/column-header-ui.js](http://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/scripts/views/data-table/column-header-ui.js).

### Server-side: Ajax Commands {#server-side-ajax-commands}

The client-side of OpenRefine gets things done by calling AJAX commands on the server-side. These commands must be registered with the OpenRefine servlet, so that the servlet knows how to route AJAX calls from the client-side. This can be done inside the `init` function in your extension's `controller.js` file, e.g.,

```
function init() {
      var RefineServlet = Packages.com.google.refine.RefineServlet;
      RefineServlet.registerCommand(module, "my-command", new Packages.com.foo.bar.MyCommand());
  }
```

Your command will then be accessible at [http://127.0.0.1:3333/command/my-extension/my-command](http://127.0.0.1:3333/command/my-extension/my-command).

### Server-side: Operations {#server-side-operations}

Most commands change the project's data. Most of them do so by creating abstract operations. See the Changes, History, Processes, and Operations section of the [Server Side Architecture](https://github.com/OpenRefine/OpenRefine/wiki/Server-Side-Architecture) document.

You can register an operation **class** in the `init` function as follows:

```
Packages.com.google.refine.operations.OperationRegistry.registerOperation(
      module, 
      "operation-name",
      Packages.com.foo.bar.MyOperation
  );
```

Do not call `new` to construct an operation instance. You must register the class itself. The class should have a static function for reconstructing an operation instance from a JSON blob:

```
static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
      ...
  }
```

### Server-side: GREL {#server-side-grel}

GREL can be extended with new functions. This is also done in the `init` function in `controller.js`, e.g.,

```
Packages.com.google.refine.grel.ControlFunctionRegistry.registerFunction(
        "functionName", new Packages.com.foo.bar.TheFunctionClass());
```

You might also want to provide new variables (beyond just `value`, `cells`, `row`, etc.) available to expressions. This is done by registering a binder that implements the interface `com.google.refine.expr.Binder`:

```
Packages.com.google.refine.expr.ExpressionUtils.registerBinder(
        new Packages.com.foo.bar.MyBinder());
```

### Server-side: Importers {#server-side-importers}

You can register an importer as follows:

```
Packages.com.google.refine.importers.ImporterRegistry.registerImporter(
      "importer-name", new Packages.com.foo.bar.MyImporter());
```

The string `"importer-name"` isn't important at all. It's not really related to file extension or mime-type. Just use something unique. Your importer will be explicitly called to test if it can import something.

### Server-side: Exporters {#server-side-exporters}

You can register an exporter as follows:

```
Packages.com.google.refine.exporters.ExporterRegistry.registerExporter(
      "exporter-name", new Packages.com.foo.bar.MyExporter());
```

The string `"exporter-name"` isn't important at all. It's only used by the client-side to tell the server-side which exporter to use. Just use something unique and, of course, relevant.

### Server-side: Overlay Models {#server-side-overlay-models}

Overlay models are objects attached onto a core Project object to store and manage additional data for that project. For example, the schema alignment skeleton is managed by the Protograph overlay model. An overlay model implements the interface `com.google.refine.model.OverlayModel` and can be registered like so:

```
Packages.com.google.refine.model.Project.registerOverlayModel(
      "model-name",
      Packages.com.foo.bar.MyOverlayModel);
```

Note that you register the **class** , not an instance. The class should implement the following static method for reconstructing an overlay model instance from a JSON blob:

```
static public OverlayModel reconstruct(JSONObject o) throws JSONException {
      ...
  }
```

When the project gets saved, the overlay model instance's `write` method will be called:

```
public void write(JSONWriter writer, Properties options) throws JSONException {
      ...
  }
```

### Server-side: Scripting Languages {#server-side-scripting-languages}

A scripting language (such as Jython) can be registered as follows:

```
Packages.com.google.refine.expr.MetaParser.registerLanguageParser(
        "jython",
        "Jython",
        Packages.com.google.refine.jython.JythonEvaluable.createParser(),
        "return value"
    );
```

The first string is the prefix that gets prepended to each expression so that we know which language the expression is in. This should be short, unique, and identifying. The second string is a user-friendly name of the language. The third is an object that implements the interface `com.google.refine.expr.LanguageSpecificParser`. The final string is the default expression in that language that would return the cell's value.

In 2018 we are making important changes to OpenRefine to modernize it, for the benefit of users and contributors. This page describes the changes that impact developers of extensions or forks and is intended to minimize the effort required on their end to follow the transition. The instructions are written specifically with extension maintainers in mind, but fork maintainers should also find it useful.

This document describes the migrations in the order they are committed to the master branch. This means that it should be possible to perform each migration in turn, with the ability to run the software between each stage by checking out the appropriate git commit.
