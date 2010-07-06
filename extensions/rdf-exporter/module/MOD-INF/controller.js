
importPackage(com.metaweb.gridworks.rdf.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
    /*
     *  Attach an rdf schema to each project.
     */
    Packages.com.metaweb.gridworks.model.Project.registerOverlayModel(
        "rdfSchema",
        Packages.com.metaweb.gridworks.rdf.RdfSchema);
    
    /*
     *  Operations
     */
    Packages.com.metaweb.gridworks.operations.OperationRegistry.register(
        "save-rdf-schema", Packages.com.metaweb.gridworks.rdf.operations.SaveRdfSchemaOperation);
    
    /*
     *  Exporters
     */
    var ExportRowsCommand = Packages.com.metaweb.gridworks.commands.project.ExportRowsCommand;
    var RdfExporter = Packages.com.metaweb.gridworks.rdf.exporters.RdfExporter;
    
    ExportRowsCommand.registerExporter("rdf", new RdfExporter("RDF/XML"));
    ExportRowsCommand.registerExporter("n3", new RdfExporter("N3"));
    
    /*
     *  GEL Functions and Binders
     */
    Packages.com.metaweb.gridworks.gel.ControlFunctionRegistry.registerFunction(
        "urlify", new Packages.com.metaweb.gridworks.rdf.expr.functions.strings.Urlify());
        
    Packages.com.metaweb.gridworks.expr.ExpressionUtils.registerBinder(
        new Packages.com.metaweb.gridworks.rdf.expr.RdfBinder());
        
    /*
     *  Commands
     */
    var GridworksServlet = Packages.com.metaweb.gridworks.GridworksServlet;
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
    var ClientSideResourceManager = Packages.com.metaweb.gridworks.ClientSideResourceManager;
    
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
