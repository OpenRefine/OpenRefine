var html = "text/html";
var encoding = "UTF-8";
var version="0.2"
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

/*
 * Function invoked to initialize the extension.
 */
function init() {
//    Packages.java.lang.System.err.println("Initializing gData extension");
//    Packages.java.lang.System.err.println(module.getMountPoint());

	Packages.com.google.refine.RefineServlet.registerCommand(
        module, "authorize", Packages.com.google.refine.extension.gdata.AuthorizeCommand());
	Packages.com.google.refine.RefineServlet.registerCommand(
	        module, "authorize2", Packages.com.google.refine.extension.gdata.AuthorizeCommand2());
	Packages.com.google.refine.RefineServlet.registerCommand(
        module, "deauthorize", Packages.com.google.refine.extension.gdata.DeAuthorizeCommand());

    // Register importer and exporter
    Packages.com.google.refine.importers.ImporterRegistry.registerImporter(
    	      "gdata-importer", new Packages.com.google.refine.extension.gdata.GDataImporter());

//    Packages.com.google.refine.exporters.ExporterRegistry.registerExporter(
//  	      "gdata-exporter", new Packages.com.google.refine.extension.gdata.GDataExporter());

    // Script files to inject into /project page
    ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "scripts/project-injection.js"
        ]
    );
    
    // Style files to inject into /project page
    ClientSideResourceManager.addPaths(
        "project/styles",
        module,
        [
            "styles/project-injection.less"
        ]
    );    
    
}

/*
 * Function invoked to handle each request in a custom way.
 */
function process(path, request, response) {
    // Analyze path and handle this request yourself.
	
    if (path == "/" || path == "") {
        var context = {};
        // here's how to pass things into the .vt templates
        context.version = version;
        send(request, response, "index.vt", context);
    }
}

function send(request, response, template, context) {
    butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
