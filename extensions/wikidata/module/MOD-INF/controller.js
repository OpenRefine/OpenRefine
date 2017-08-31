
importPackage(org.openrefine.wikidata.commands);

/*
 * Function invoked to initialize the extension.
 */
function init() {
    var RefineServlet = Packages.com.google.refine.RefineServlet;
    RefineServlet.registerClassMapping(
            "org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange",
            "org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange");
    
    RefineServlet.cacheClass(Packages.org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation$WikibaseSchemaChange);

    /*
     * Context Initialization. This is mainly to allow testability. a simple attempt to mimic dependency injection
     */

    /*
    var initializer = new Packages.org.deri.grefine.rdf.app.InitilizationCommand();
    RefineServlet.registerCommand(module, "initialize", initializer);
    var ctxt = new Packages.org.deri.grefine.rdf.app.ApplicationContext();
    initializer.initRdfExportApplicationContext(ctxt);
    */
    
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
    
    /*
     *  Exporters
     */
   /*
    var ExporterRegistry = Packages.com.google.refine.exporters.ExporterRegistry;
    var QSV2Exporter = Packages.org.openrefine.wikidata.exporters.QuickStatements2Exporter;
    
    ExporterRegistry.registerExporter("qsv2", new QSV2());
   */

    /*
     * Commands
     */
    RefineServlet.registerCommand(module, "save-wikibase-schema", new SaveWikibaseSchemaCommand());
     
    /*
     * Resources
     */
    ClientSideResourceManager.addPaths(
      "project/scripts",
      module,
      [
        "scripts/menu-bar-extension.js",
        "scripts/dialogs/schema-alignment-dialog.js",
      ]);

    ClientSideResourceManager.addPaths(
      "project/styles",
      module,
      [
        "styles/dialogs/schema-alignment-dialog.less",
      ]);
   
}

function process(path, request, response) {
    // Analyze path and handle this request yourself.
    /*
	var loggerFactory = Packages.org.slf4j.LoggerFactory;
	var logger = loggerFactory.getLogger("rdf_extension");
    var method = request.getMethod();
    
    logger.info('receiving request for ' + path);
    if(rdfReconcileExtension.isKnownRequestUrl(path)){
    	var command = rdfReconcileExtension.getCommand(path, request);
    	logger.info('command is ' + command);
    	var serviceName = rdfReconcileExtension.getServiceName(path);
    	logger.info('command is ' + command + ', while service name is ' + serviceName);
    	if(command && command !== 'unknown'){
    		var jsonResponse;
    		if(command==='metadata'){
    			jsonResponse = GRefineServiceManager.singleton.metadata(serviceName,request);
    		}else if(command==='multi-reconcile'){
    			jsonResponse = GRefineServiceManager.singleton.multiReconcile(serviceName,request);
    		}else if (command==='suggest-type'){
    			jsonResponse = GRefineServiceManager.singleton.suggestType(serviceName,request);
    		}else if (command==='flyout-type'){
    			jsonResponse = GRefineServiceManager.singleton.previewType(serviceName,request);
    		}else if (command==='suggest-property'){
    			jsonResponse = GRefineServiceManager.singleton.suggestProperty(serviceName,request);
    		}else if (command==='flyout-property'){
    			jsonResponse = GRefineServiceManager.singleton.previewProperty(serviceName,request);
    		}else if (command==='suggest-entity'){
    			jsonResponse = GRefineServiceManager.singleton.suggestEntity(serviceName,request);
    		}else if (command==='flyout-entity'){
    			jsonResponse = GRefineServiceManager.singleton.previewEntity(serviceName,request);
    		}else if (command==='preview-resource-template'){
    			var htmlResponse = GRefineServiceManager.singleton.getHtmlOfResourcePreviewTemplate(serviceName,request);
    			if(htmlResponse){
    				butterfly.sendString(request, response, htmlResponse ,"UTF-8", "text/html");
    			}else{
    				butterfly.sendError(request, response, 404, "unknown service");
    			}
    			return;
    		}else if (command==='view-resource'){
    			var id = request.getParameter('id');
    			butterfly.redirect(request,response,id);
    			return;
    		}else if (command ==='preview-resource'){
    			logger.info("id is " + request.getParameter("id"));
    			var htmlResponse = GRefineServiceManager.singleton.previewResource(serviceName,request);
    			if(htmlResponse){
    				butterfly.sendString(request, response, htmlResponse ,"UTF-8", "text/html");
    			}else{
    				butterfly.sendError(request, response, 404, "unknown service");
    			}
    			return;
    		}
    		
    		if(jsonResponse){
    			logger.info(jsonResponse);
    			butterfly.sendString(request, response, jsonResponse ,"UTF-8", "text/javascript");
    			return;
    		}else{
    			butterfly.sendError(request, response, 404, "unknown service");
    		}
    	}
    	//else it is an unknown command... do nothing
    }
    
    if (path == "/" || path == "") {
    	butterfly.redirect(request, response, "index.html");
    } */
}
