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

var html = "text/html";
var encoding = "UTF-8";
var version = "0.2";

/*
 * Function invoked to initialize the extension.
 */
function init() {
//    Packages.java.lang.System.err.println("Initializing gData extension");
//    Packages.java.lang.System.err.println(module.getMountPoint());

    var RS = Packages.com.google.refine.RefineServlet;
    RS.registerCommand(module, "authorize", Packages.com.google.refine.extension.gdata.AuthorizeCommand());
    RS.registerCommand(module, "authorize2", Packages.com.google.refine.extension.gdata.AuthorizeCommand2());
    RS.registerCommand(module, "deauthorize", Packages.com.google.refine.extension.gdata.DeAuthorizeCommand());
    
    // Register importer and exporter
    var IM = Packages.com.google.refine.importing.ImportingManager;
    IM.registerFormat("service/gdata", "GData services"); // generic format, no parser to handle it
    IM.registerFormat("service/gdata/spreadsheet", "Google spreadsheets", false, "GoogleSpreadsheetParserUI",
        new Packages.com.google.refine.extension.gdata.GDataImporter());
    IM.registerUrlRewriter(new Packages.com.google.refine.extension.gdata.GDataUrlRewriter())
    IM.registerUrlRewriter(new Packages.com.google.refine.extension.gdata.FusionTablesUrlRewriter())

//    Packages.com.google.refine.exporters.ExporterRegistry.registerExporter(
//            "gdata-exporter", new Packages.com.google.refine.extension.gdata.GDataExporter());

    // Script files to inject into /project page
    var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;
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
