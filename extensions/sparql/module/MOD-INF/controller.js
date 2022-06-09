/*
 * Controller for SPARQL extension.
 * 
 * This is run in the Butterfly (ie Refine) server context using the Rhino
 * Javascript interpreter.
 */

var html = "text/html";
var encoding = "UTF-8";
var version = "0.1";
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

/*
 * Function invoked to initialize the extension.
 */
function init() {
 
  // Script files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/scripts",
    module,
    [
      "scripts/index/sparql-import-controller.js",
      "scripts/index/parsing-panel.js",
      "scripts/index/sparql-source-ui.js",
      "scripts/index/wikidata/Sparql.js"
    ]
  );
  
  // Script files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/index/sparql-import-controller.js",
      "scripts/index/parsing-panel.js",
      "scripts/index/sparql-source-ui.js",
      "scripts/index/wikidata/Sparql.js"
    ]
  );
  
  // Style files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/styles",
    module,
    [
      "styles/sparql-import.less",
      "styles/theme.less"
    ]
  );
  
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
