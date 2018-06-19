/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

/*
 * Controller for GData extension.
 * 
 * This is run in the Butterfly (ie Refine) server context using the Rhino
 * Javascript interpreter.
 */

var html = "text/html";
var encoding = "UTF-8";
var version = "0.2";
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

/*
 * Function invoked to initialize the extension.
 */
function init() {
  var RS = Packages.com.google.refine.RefineServlet;
  RS.registerCommand(module, "deauthorize", Packages.com.google.refine.extension.gdata.DeAuthorizeCommand());
  RS.registerCommand(module, "upload", Packages.com.google.refine.extension.gdata.UploadCommand());
  // TODO: Need a new OAUTH2 authorize command for FusionTables

  // Register importer and exporter
  var IM = Packages.com.google.refine.importing.ImportingManager;
  
  IM.registerController(
    module,
    "gdata-importing-controller",
    new Packages.com.google.refine.extension.gdata.GDataImportingController()
  );
  

//Packages.com.google.refine.exporters.ExporterRegistry.registerExporter(
//"gdata-exporter", new Packages.com.google.refine.extension.gdata.GDataExporter());

  // Script files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/scripts",
    module,
    [
      "scripts/gdata-extension.js",
      "scripts/index/importing-controller.js",
      "scripts/index/gdata-source-ui.js"
    ]
  );
  // Style files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/styles",
    module,
    [
      "styles/importing-controller.less"
    ]
  );
  
  // Script files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/gdata-extension.js",
      "scripts/project/exporters.js"
    ]
  );
}

/*
 * Function invoked to handle each request in a custom way.
 */
function process(path, request, response) {
  // Analyze path and handle this request yourself.
  if (path == "authorize") {
    var context = {};
    context.authorizationUrl = Packages.com.google.refine.extension.gdata.GDataExtension.getAuthorizationUrl(module, request);
    
    send(request, response, "authorize.vt", context);
  } else if (path == "authorized") {
    var context = {};
    context.winname = request.getParameter("winname");
    context.callback = request.getParameter("cb");
    
    (function() {
      var token =  Packages.com.google.refine.extension.gdata.GDataExtension.getTokenFromCode(module,request);
      if (token) {
        Packages.com.google.refine.extension.gdata.TokenCookie.setToken(request, response, token);
        return;
      }
      Packages.com.google.refine.extension.gdata.TokenCookie.deleteToken(request, response);
    })();
    
    send(request, response, "authorized.vt", context);
  } else if (path == "/" || path == "") {
      var context = {};
      context.version = version;
      send(request, response, "index.vt", context);
  } 
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
