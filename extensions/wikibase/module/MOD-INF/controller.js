importPackage(org.openrefine.wikibase.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
    var RefineServlet = Packages.com.google.refine.RefineServlet;
    RefineServlet.registerClassMapping(
            "org.openrefine.wikibase.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange",
            "org.openrefine.wikibase.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange");
    RefineServlet.registerClassMapping(
            "org.openrefine.wikibase.operations.PerformWikibaseEditsOperation$PerformWikibaseEditsChange",
            "org.openrefine.wikibase.operations.PerformWikibaseEditsOperation$PerformWikibaseEditsChange");
    
    RefineServlet.cacheClass(Packages.org.openrefine.wikibase.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange);
    RefineServlet.cacheClass(Packages.org.openrefine.wikibase.operations.PerformWikibaseEditsOperation$PerformWikibaseEditsChange);

    /*
     *  Attach a Wikibase schema to each project.
     */
    Packages.com.google.refine.model.Project.registerOverlayModel(
        "wikibaseSchema",
        Packages.org.openrefine.wikibase.schema.WikibaseSchema);
    
    /*
     *  Operations
     */
    Packages.com.google.refine.operations.OperationRegistry.registerOperation(
        module, "save-wikibase-schema", Packages.org.openrefine.wikibase.operations.SaveWikibaseSchemaOperation);
    Packages.com.google.refine.operations.OperationRegistry.registerOperation(
        module, "perform-wikibase-edits", Packages.org.openrefine.wikibase.operations.PerformWikibaseEditsOperation);
    
    /*
     *  Exporters
     */
    var ExporterRegistry = Packages.com.google.refine.exporters.ExporterRegistry;
    var QSExporter = Packages.org.openrefine.wikibase.exporters.QuickStatementsExporter;
    var SchemaExporter = Packages.org.openrefine.wikibase.exporters.SchemaExporter;
    
    ExporterRegistry.registerExporter("quickstatements", new QSExporter());
    ExporterRegistry.registerExporter("wikibase-schema", new SchemaExporter());

    /*
     * Commands
     */
    RefineServlet.registerCommand(module, "save-wikibase-schema", new SaveWikibaseSchemaCommand());
    RefineServlet.registerCommand(module, "preview-wikibase-schema", new PreviewWikibaseSchemaCommand());
    RefineServlet.registerCommand(module, "perform-wikibase-edits", new PerformWikibaseEditsCommand());
    RefineServlet.registerCommand(module, "parse-wikibase-schema", new ParseWikibaseSchemaCommand());
    RefineServlet.registerCommand(module, "login", new LoginCommand());

    /*
     * GREL functions
     */
    var CFR = Packages.com.google.refine.grel.ControlFunctionRegistry;
    CFR.registerFunction("wikibaseIssues", new Packages.org.openrefine.wikibase.functions.WikibaseIssuesFunction());

    /*
     * Resources
     */
    ClientSideResourceManager.addPaths(
      "project/scripts",
      module,
      [
        "scripts/ajv.min.js",
        "scripts/wikidata-manifest-v1.0.js",
        "scripts/commons-manifest-v2.0.js",
        "scripts/wikibase-manifest-schema-v1.js",
        "scripts/wikibase-manifest-schema-v2.js",
        "scripts/wikibase-manager.js",
        "scripts/template-manager.js",
        "scripts/menu-bar-extension.js",
        "scripts/warnings-renderer.js",
        "scripts/lang-suggest.js",
        "scripts/better-suggest.js",
        "scripts/preview-renderer.js",
        "scripts/wikibase-suggest.js",
        "scripts/schema-alignment.js",
        "scripts/wikidata-extension-manager.js",
        "scripts/dialogs/manage-account-dialog.js",
        "scripts/dialogs/perform-edits-dialog.js",
        "scripts/dialogs/import-schema-dialog.js",
        "scripts/dialogs/wikibase-dialog.js",
        "scripts/dialogs/statement-configuration-dialog.js",
        "scripts/dialogs/save-schema-dialog.js",
        "scripts/dialogs/schema-management-dialog.js",
        "scripts/jquery.uls.data.js",
      ]);

    ClientSideResourceManager.addPaths(
      "project/styles",
      module,
      [
        "styles/schema-alignment.css",
        "styles/dialogs/manage-account-dialog.css",
        "styles/dialogs/import-schema-dialog.css",
        "styles/dialogs/perform-edits.css",
        "styles/dialogs/wikibase-dialog.css",
        "styles/dialogs/add-wikibase-dialog.css",
        "styles/dialogs/statement-configuration-dialog.css",
        "styles/dialogs/save-schema-dialog.css",
        "styles/dialogs/schema-management-dialog.css"
      ]);
   
}

