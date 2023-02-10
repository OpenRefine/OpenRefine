/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Controller for JDBC Database extension.
 * 
 * This is run in the Butterfly (ie Refine) server context using the Rhino
 * Javascript interpreter.
 */

var html = "text/html";
var encoding = "UTF-8";
var version = "0.1";
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

var logger = Packages.org.slf4j.LoggerFactory.getLogger("database-extension"),
File = Packages.java.io.File,
refineServlet = Packages.com.google.refine.RefineServlet;

/*
 * Register our custom commands.
 */
function registerCommands() {
  
  logger.trace("Registering Database Extension Commands......");
  var RS = Packages.com.google.refine.RefineServlet;
  RS.registerCommand(module, "test-connect", Packages.com.google.refine.extension.database.cmd.TestConnectCommand());
  RS.registerCommand(module, "connect", Packages.com.google.refine.extension.database.cmd.ConnectCommand());
  RS.registerCommand(module, "saved-connection", Packages.com.google.refine.extension.database.cmd.SavedConnectionCommand());
  RS.registerCommand(module, "execute-query", Packages.com.google.refine.extension.database.cmd.ExecuteQueryCommand());
  RS.registerCommand(module, "test-query", Packages.com.google.refine.extension.database.cmd.TestQueryCommand());
  logger.trace("Database Extension Command Registration done!!");
}

function registerOperations() {
}

function registerFunctions() {
}


/*
 * Function invoked to initialize the extension.
 */
function init() {
  
  logger.trace("Initializing OpenRefine Database Extension...");
  logger.trace("Database Extension Mount point " + module.getMountPoint());

  registerCommands();
  registerOperations();
  registerFunctions();
 

  // Register importer and exporter
  var IM = Packages.com.google.refine.importing.ImportingManager;
  
  IM.registerController(
    module,
    "database-import-controller",
    new Packages.com.google.refine.extension.database.DatabaseImportController()
  );


  // Script files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/scripts",
    module,
    [
      "scripts/index/jquery.contextMenu.js",
      "scripts/index/jquery.ui.position.js",
      "scripts/database-extension.js",
      "scripts/index/database-import-controller.js",
      "scripts/index/database-source-ui.js"
    ]
  );
  // Style files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/styles",
    module,
    [
      "styles/jquery.contextMenu.css",
      "styles/database-import.css"

    ]
  );
  
  // Script files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/database-extension.js",
      "scripts/project/database-exporters.js"
    ]
  );
}

/*
 * Function invoked to handle each request in a custom way.
 */
function process(path, request, response) {
  
  
   var method = request.getMethod();
    
   logger.trace('receiving request for ' + path);	
  
  if (path == "/" || path == "") {
      var context = {};
      context.version = version;
      send(request, response, "index.vt", context);
  } 
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
