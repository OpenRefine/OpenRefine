
importPackage(com.google.refine.rdf.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
	var RefineServlet = Packages.com.google.refine.RefineServlet;
	/*
     * Context Initialization. This is mainly to allow testability. a simple attempt to mimic dependency injection
     */
    var initializer = new Packages.com.google.refine.rdf.app.InitilizationCommand();
    RefineServlet.registerCommand(module, "initialize", initializer);
    var ctxt = new Packages.com.google.refine.rdf.app.ApplicationContext();
    initializer.initRdfExportApplicationContext(ctxt);
    
    /*
     *  Attach an rdf schema to each project.
     */
    Packages.com.google.refine.model.Project.registerOverlayModel(
        "rdfSchema",
        Packages.com.google.refine.rdf.RdfSchema);
    
    /*
     *  Operations
     */
    Packages.com.google.refine.operations.OperationRegistry.registerOperation(
        module, "save-rdf-schema", Packages.com.google.refine.rdf.operations.SaveRdfSchemaOperation);
    
    /*
     *  Exporters
     */
    var ExporterRegistry = Packages.com.google.refine.exporters.ExporterRegistry;
    var RdfExporter = Packages.com.google.refine.rdf.exporters.RdfExporter;
    
    ExporterRegistry.registerExporter("rdf", new RdfExporter(ctxt,org.openrdf.rio.RDFFormat.RDFXML));
    ExporterRegistry.registerExporter("n3", new RdfExporter(ctxt,org.openrdf.rio.RDFFormat.TURTLE));
    
    /*
     *  GEL Functions and Binders
     */
    Packages.com.google.refine.gel.ControlFunctionRegistry.registerFunction(
        "urlify", new Packages.com.google.refine.rdf.expr.functions.strings.Urlify());
        
    Packages.com.google.refine.expr.ExpressionUtils.registerBinder(
        new Packages.com.google.refine.rdf.expr.RdfBinder(ctxt));
        
    /*
     *  Commands
     */
    RefineServlet.registerCommand(module, "save-rdf-schema", new SaveRdfSchemaCommand(ctxt));
    RefineServlet.registerCommand(module, "preview-rdf", new PreviewRdfCommand());
    RefineServlet.registerCommand(module, "save-baseURI", new SaveBaseURICommand(ctxt));
    RefineServlet.registerCommand(module, "suggest-term", new SuggestTermCommand(ctxt));
    RefineServlet.registerCommand(module, "add-prefix", new AddPrefixCommand(ctxt));
    RefineServlet.registerCommand(module, "list-vocabularies", new ListVocabulariesCommand(ctxt));
    RefineServlet.registerCommand(module, "delete-vocabulary", new DeleteVocabularyCommand(ctxt));
    RefineServlet.registerCommand(module, "refresh-vocabulary", new RefreshVocabularyCommand(ctxt));
    RefineServlet.registerCommand(module, "preview-rdf-expression", new PreviewRdfValueExpressionCommand());
    RefineServlet.registerCommand(module, "list-prefixes", new ListPrefixesCommand(ctxt));
       
    /*
     *  Client-side Resources
     */
    var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;
    
    // Script files to inject into /project page
    ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "scripts/rdf-data-table-view.js",
            "scripts/menu-bar-extensions.js",
            "scripts/rdf-schema-alignment.js",
            "scripts/rdf-schema-alignment-ui-node.js",
            "scripts/rdf-schema-alignment-ui-link.js",
            "scripts/suggestterm.suggest.js",
            "scripts/rdf-schema-vocabulary-manager.js"
        ]
    );
    
    // Style files to inject into /project page
    ClientSideResourceManager.addPaths(
        "project/styles",
        module,
        [
            "styles/rdf-schema-alignment-dialog.css",
        ]
    );
    
}
