
importPackage(com.google.gridworks.rdf.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
    /*
     *  Attach an rdf schema to each project.
     */
    Packages.com.google.gridworks.model.Project.registerOverlayModel(
        "rdfSchema",
        Packages.com.google.gridworks.rdf.RdfSchema);
    
    /*
     *  Operations
     */
    Packages.com.google.gridworks.operations.OperationRegistry.register(
        "save-rdf-schema", Packages.com.google.gridworks.rdf.operations.SaveRdfSchemaOperation);
    
    /*
     *  Exporters
     */
    var ExporterRegistry = Packages.com.google.gridworks.exporters.ExporterRegistry;
    var RdfExporter = Packages.com.google.gridworks.rdf.exporters.RdfExporter;
    
    ExporterRegistry.registerExporter("rdf", new RdfExporter("RDF/XML"));
    ExporterRegistry.registerExporter("n3", new RdfExporter("N3"));
    
    /*
     *  GEL Functions and Binders
     */
    Packages.com.google.gridworks.gel.ControlFunctionRegistry.registerFunction(
        "urlify", new Packages.com.google.gridworks.rdf.expr.functions.strings.Urlify());
        
    Packages.com.google.gridworks.expr.ExpressionUtils.registerBinder(
        new Packages.com.google.gridworks.rdf.expr.RdfBinder());
        
    /*
     *  Commands
     */
    var GridworksServlet = Packages.com.google.gridworks.GridworksServlet;
    GridworksServlet.registerCommand("save-rdf-schema", new SaveRdfSchemaCommand());
    GridworksServlet.registerCommand("preview-rdf",new PreviewRdfCommand());
    GridworksServlet.registerCommand("save-baseURI",new SaveBaseURI());
    GridworksServlet.registerCommand("suggest-term",new SuggestTermCommand());
    GridworksServlet.registerCommand("import-vocabulary",new ImportVocabularyCommand());
    GridworksServlet.registerCommand("list-vocabularies",new ListVocabulariesCommand());
    GridworksServlet.registerCommand("delete-vocabulary",new DeleteVocabularyCommand());
        
    /*
     *  Client-side Resources
     */
    var ClientSideResourceManager = Packages.com.google.gridworks.ClientSideResourceManager;
    
    // Script files to inject into /project page
    ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "scripts/menu-bar-extensions.js",
            "scripts/rdf-schema-alignment.js",
            "scripts/rdf-schema-alignment-ui-node.js",
            "scripts/rdf-schema-alignment-ui-link.js",
            "scripts/suggestterm.suggest.js"
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
