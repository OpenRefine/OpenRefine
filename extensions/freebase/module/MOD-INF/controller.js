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

function init() {
  Packages.com.google.refine.freebase.oauth.FreebaseProvider.register();

  var RS = Packages.com.google.refine.RefineServlet;
  RS.registerClassMapping(
      "com.google.refine.model.changes.DataExtensionChange",
  "com.google.refine.freebase.model.changes.DataExtensionChange");
  RS.registerClassMapping(
      "com.google.refine.operations.SaveProtographOperation$ProtographChange",
  "com.google.refine.freebase.operations.SaveProtographOperation$ProtographChange");

  // TODO(dfhuynh): Temporary hack until we know how the core module can resolve our module's classes
  RS.cacheClass(Packages.com.google.refine.freebase.model.changes.DataExtensionChange);
  RS.cacheClass(Packages.com.google.refine.freebase.operations.SaveProtographOperation$ProtographChange);

  RS.registerCommand(module, "extend-data",           new Packages.com.google.refine.freebase.commands.ExtendDataCommand());
  RS.registerCommand(module, "preview-extend-data",   new Packages.com.google.refine.freebase.commands.PreviewExtendDataCommand());

  RS.registerCommand(module, "preview-protograph",    new Packages.com.google.refine.freebase.commands.PreviewProtographCommand());
  RS.registerCommand(module, "save-protograph",       new Packages.com.google.refine.freebase.commands.SaveProtographCommand());

  RS.registerCommand(module, "check-authorization",   new Packages.com.google.refine.freebase.commands.auth.CheckAuthorizationCommand());
  RS.registerCommand(module, "authorize",             new Packages.com.google.refine.freebase.commands.auth.AuthorizeCommand());
  RS.registerCommand(module, "deauthorize",           new Packages.com.google.refine.freebase.commands.auth.DeAuthorizeCommand());
  RS.registerCommand(module, "user-badges",           new Packages.com.google.refine.freebase.commands.auth.GetUserBadgesCommand());

  RS.registerCommand(module, "upload-data",           new Packages.com.google.refine.freebase.commands.UploadDataCommand());
  RS.registerCommand(module, "import-qa-data",        new Packages.com.google.refine.freebase.commands.ImportQADataCommand());
  RS.registerCommand(module, "mqlread",               new Packages.com.google.refine.freebase.commands.MQLReadCommand());
  RS.registerCommand(module, "mqlwrite",              new Packages.com.google.refine.freebase.commands.MQLWriteCommand());

  var OR = Packages.com.google.refine.operations.OperationRegistry;

  OR.registerOperation(module, "extend-data",                     Packages.com.google.refine.freebase.operations.ExtendDataOperation);
  OR.registerOperation(module, "import-qa-data",                  Packages.com.google.refine.freebase.operations.ImportQADataOperation);
  OR.registerOperation(module, "save-protograph",                 Packages.com.google.refine.freebase.operations.SaveProtographOperation); // for backward compatibility
  OR.registerOperation(module, "save-schema-alignment-skeleton",  Packages.com.google.refine.freebase.operations.SaveProtographOperation);

  var RC = Packages.com.google.refine.model.recon.ReconConfig;

  RC.registerReconConfig(module, "strict", Packages.com.google.refine.freebase.model.recon.StrictReconConfig);
  RC.registerReconConfig(module, "extend", Packages.com.google.refine.freebase.model.recon.DataExtensionReconConfig);

  var ER = Packages.com.google.refine.exporters.ExporterRegistry;

  ER.registerExporter("tripleloader", new Packages.com.google.refine.freebase.ProtographTransposeExporter.TripleLoaderExporter());
  ER.registerExporter("mqlwrite", new Packages.com.google.refine.freebase.ProtographTransposeExporter.MqlwriteLikeExporter());

  var FCR = Packages.com.google.refine.grel.ControlFunctionRegistry;

  FCR.registerFunction("mqlKeyQuote", new Packages.com.google.refine.freebase.expr.MqlKeyQuote());
  FCR.registerFunction("mqlKeyUnquote", new Packages.com.google.refine.freebase.expr.MqlKeyUnquote());

  Packages.com.google.refine.model.Project.
  registerOverlayModel("freebaseProtograph", Packages.com.google.refine.freebase.protograph.Protograph);

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/extension.js",

      "scripts/util/sign.js",
      "scripts/util/freebase.js",

      "scripts/dialogs/freebase-loading-dialog.js",
      "scripts/dialogs/extend-data-preview-dialog.js",

      "scripts/dialogs/schema-alignment/dialog.js",
      "scripts/dialogs/schema-alignment/ui-node.js",
      "scripts/dialogs/schema-alignment/ui-link.js"
    ]
  );

  ClientSideResourceManager.addPaths(
    "project/styles",
    module,
    [
      "styles/dialogs/freebase-loading-dialog.less",
      "styles/dialogs/extend-data-preview-dialog.less",
      "styles/dialogs/schema-alignment-dialog.less"
    ]
  );
}
