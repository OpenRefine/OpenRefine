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
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;
var RS = Packages.com.google.refine.RefineServlet;

/*
 * Function invoked to initialize the extension.
 */
function init() {
  // Packages.java.lang.System.err.println("Initializing sample extension");
 
	//var OR = Packages.com.google.refine.operations.OperationRegistry;

  RS.registerCommand(module, "create-crowdflower-job", new Packages.com.google.refine.crowdsourcing.crowdflower.CreateNewJobCommand());
  RS.registerCommand(module, "preview-crowdflower-jobs", new Packages.com.google.refine.crowdsourcing.crowdflower.PreviewExistingJobsCommand());
  RS.registerCommand(module, "copy-crowdflower-job", new Packages.com.google.refine.crowdsourcing.crowdflower.CopyJobCommand());
  RS.registerCommand(module, "get-crowdflower-job", new Packages.com.google.refine.crowdsourcing.crowdflower.GetJobInfoCommand());
  // Script files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/extension.js",
      "scripts/dialogs/crowdflower-api-settings-dialog.js",
      "scripts/dialogs/crowdflower-job-columns-dialog.js",
      "scripts/dialogs/crowdflower-create-gold-dialog.js"
    ]
  );

  // Style files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/styles",
    module,
    [
      "styles/theme.less",
      "styles/dialogs/crowdflower-gui.less",
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
    context.someList = ["Superior","Michigan","Huron","Erie","Ontario"];
    context.someString = "foo";
    context.someInt = Packages.com.google.refine.sampleExtension.SampleUtil.stringArrayLength(context.someList);

    send(request, response, "index.vt", context);
  }
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
