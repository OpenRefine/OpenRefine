importPackage(org.openrefine.wikidata.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
    var RefineServlet = Packages.com.google.refine.RefineServlet;
    RefineServlet.registerClassMapping(
            "org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange",
            "org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange");
    RefineServlet.registerClassMapping(
            "org.openrefine.wikidata.operations.PerformWikibaseEditsOperation$PerformWikibaseEditsChange",
            "org.openrefine.wikidata.operations.PerformWikibaseEditsOperation$PerformWikibaseEditsChange");
    
    RefineServlet.cacheClass(Packages.org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange);
    RefineServlet.cacheClass(Packages.org.openrefine.wikidata.operations.PerformWikibaseEditsOperation$PerformWikibaseEditsChange);

    /*
     *  Attach a Wikibase schema to each project.
     */
    Packages.com.google.refine.model.Project.registerOverlayModel(
        "wikibaseSchema",
        Packages.org.openrefine.wikidata.schema.WikibaseSchema);
    
    /*
     *  Operations
     */
    Packages.com.google.refine.operations.OperationRegistry.registerOperation(
        module, "save-wikibase-schema", Packages.org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation);
    Packages.com.google.refine.operations.OperationRegistry.registerOperation(
        module, "perform-wikibase-edits", Packages.org.openrefine.wikidata.operations.PerformWikibaseEditsOperation);
    
    /*
     *  Exporters
     */
    var ExporterRegistry = Packages.com.google.refine.exporters.ExporterRegistry;
    var QSExporter = Packages.org.openrefine.wikidata.exporters.QuickStatementsExporter;
    var SchemaExporter = Packages.org.openrefine.wikidata.exporters.SchemaExporter;
    
    ExporterRegistry.registerExporter("quickstatements", new QSExporter());
    ExporterRegistry.registerExporter("wikibase-schema", new SchemaExporter());

    /*
     * Commands
     */
    RefineServlet.registerCommand(module, "save-wikibase-schema", new SaveWikibaseSchemaCommand());
    RefineServlet.registerCommand(module, "preview-wikibase-schema", new PreviewWikibaseSchemaCommand());
    RefineServlet.registerCommand(module, "perform-wikibase-edits", new PerformWikibaseEditsCommand());
    RefineServlet.registerCommand(module, "login", new LoginCommand());

    /*
     * Resources
     */
    ClientSideResourceManager.addPaths(
      "project/scripts",
      module,
      [
        "scripts/ajv.min.js",
        "scripts/wikidata-manifest-v1.0.js",
        "scripts/wikibase-manifest-schema-v1.js",
        "scripts/wikibase-manifest-schema-v2.js",
        "scripts/wikibase-manager.js",
        "scripts/menu-bar-extension.js",
        "scripts/warningsrenderer.js",
        "scripts/langsuggest.js",
        "scripts/bettersuggest.js",
        "scripts/previewrenderer.js",
        "scripts/wikibase-suggest.js",
        "scripts/schema-alignment.js",
        "scripts/wikidata-extension-manager.js",
        "scripts/dialogs/manage-account-dialog.js",
        "scripts/dialogs/perform-edits-dialog.js",
        "scripts/dialogs/import-schema-dialog.js",
        "scripts/dialogs/wikibase-dialog.js",
        "scripts/dialogs/statement-configuration-dialog.js",
        "scripts/jquery.uls.data.js",
      ]);

    ClientSideResourceManager.addPaths(
      "project/styles",
      module,
      [
        "styles/theme.less",
        "styles/schema-alignment.less",
        "styles/dialogs/manage-account-dialog.less",
        "styles/dialogs/import-schema-dialog.less",
        "styles/dialogs/perform-edits.less",
        "styles/dialogs/wikibase-dialog.less",
        "styles/dialogs/add-wikibase-dialog.less",
        "styles/dialogs/statement-configuration-dialog.less"
      ]);
   
}

