/*

Copyright 2010,2012 Google Inc.
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
var bundle = true;

var templatedFiles = {
  // Requests with last path segments mentioned here 
  // will get served from .vt files with the same names
  "index" : {
    outputEncodings: true
  },
  "preferences" : {
    outputEncodings: false
  },
  "project" : {
    outputEncodings: true
  }
};

function registerCommands() {
  var RS = Packages.com.google.refine.RefineServlet;

  RS.registerCommand(module, "get-version", new Packages.com.google.refine.commands.GetVersionCommand());
  RS.registerCommand(module, "get-csrf-token", new Packages.com.google.refine.commands.GetCSRFTokenCommand());

  RS.registerCommand(module, "get-importing-configuration", new Packages.com.google.refine.commands.importing.GetImportingConfigurationCommand());
  RS.registerCommand(module, "create-importing-job", new Packages.com.google.refine.commands.importing.CreateImportingJobCommand());
  RS.registerCommand(module, "get-importing-job-status", new Packages.com.google.refine.commands.importing.GetImportingJobStatusCommand());
  RS.registerCommand(module, "importing-controller", new Packages.com.google.refine.commands.importing.ImportingControllerCommand());
  RS.registerCommand(module, "cancel-importing-job", new Packages.com.google.refine.commands.importing.CancelImportingJobCommand());

  RS.registerCommand(module, "create-project-from-upload", new Packages.com.google.refine.commands.project.CreateProjectCommand());
  RS.registerCommand(module, "import-project", new Packages.com.google.refine.commands.project.ImportProjectCommand());
  RS.registerCommand(module, "export-project", new Packages.com.google.refine.commands.project.ExportProjectCommand());
  RS.registerCommand(module, "export-rows", new Packages.com.google.refine.commands.project.ExportRowsCommand());

  RS.registerCommand(module, "get-project-metadata", new Packages.com.google.refine.commands.project.GetProjectMetadataCommand());
  RS.registerCommand(module, "get-all-project-metadata", new Packages.com.google.refine.commands.workspace.GetAllProjectMetadataCommand());
  RS.registerCommand(module, "set-project-metadata", new Packages.com.google.refine.commands.project.SetProjectMetadataCommand());
  RS.registerCommand(module, "get-all-project-tags", new Packages.com.google.refine.commands.workspace.GetAllProjectTagsCommand());
  RS.registerCommand(module, "set-project-tags", new Packages.com.google.refine.commands.project.SetProjectTagsCommand());

  RS.registerCommand(module, "delete-project", new Packages.com.google.refine.commands.project.DeleteProjectCommand());
  RS.registerCommand(module, "rename-project", new Packages.com.google.refine.commands.project.RenameProjectCommand());
  
  RS.registerCommand(module, "get-models", new Packages.com.google.refine.commands.project.GetModelsCommand());
  RS.registerCommand(module, "get-rows", new Packages.com.google.refine.commands.row.GetRowsCommand());
  RS.registerCommand(module, "get-processes", new Packages.com.google.refine.commands.history.GetProcessesCommand());
  RS.registerCommand(module, "get-history", new Packages.com.google.refine.commands.history.GetHistoryCommand());
  RS.registerCommand(module, "get-operations", new Packages.com.google.refine.commands.history.GetOperationsCommand());
  RS.registerCommand(module, "get-columns-info", new Packages.com.google.refine.commands.column.GetColumnsInfoCommand());
  RS.registerCommand(module, "get-scatterplot", new Packages.com.google.refine.commands.browsing.GetScatterplotCommand());

  RS.registerCommand(module, "undo-redo", new Packages.com.google.refine.commands.history.UndoRedoCommand());
  RS.registerCommand(module, "apply-operations", new Packages.com.google.refine.commands.history.ApplyOperationsCommand());
  RS.registerCommand(module, "cancel-processes", new Packages.com.google.refine.commands.history.CancelProcessesCommand());

  RS.registerCommand(module, "compute-facets", new Packages.com.google.refine.commands.browsing.ComputeFacetsCommand());
  RS.registerCommand(module, "compute-clusters", new Packages.com.google.refine.commands.browsing.ComputeClustersCommand());
  RS.registerCommand(module, "get-clustering-functions-and-distances", new Packages.com.google.refine.commands.browsing.GetClusteringFunctionsAndDistancesCommand());

  RS.registerCommand(module, "edit-one-cell", new Packages.com.google.refine.commands.cell.EditOneCellCommand());
  RS.registerCommand(module, "text-transform", new Packages.com.google.refine.commands.cell.TextTransformCommand());
  RS.registerCommand(module, "mass-edit", new Packages.com.google.refine.commands.cell.MassEditCommand());
  RS.registerCommand(module, "join-multi-value-cells", new Packages.com.google.refine.commands.cell.JoinMultiValueCellsCommand());
  RS.registerCommand(module, "split-multi-value-cells", new Packages.com.google.refine.commands.cell.SplitMultiValueCellsCommand());
  RS.registerCommand(module, "fill-down", new Packages.com.google.refine.commands.cell.FillDownCommand());
  RS.registerCommand(module, "blank-down", new Packages.com.google.refine.commands.cell.BlankDownCommand());
  RS.registerCommand(module, "transpose-columns-into-rows", new Packages.com.google.refine.commands.cell.TransposeColumnsIntoRowsCommand());
  RS.registerCommand(module, "transpose-rows-into-columns", new Packages.com.google.refine.commands.cell.TransposeRowsIntoColumnsCommand());
  RS.registerCommand(module, "key-value-columnize", new Packages.com.google.refine.commands.cell.KeyValueColumnizeCommand());

  RS.registerCommand(module, "get-languages", Packages.com.google.refine.commands.lang.GetLanguagesCommand());
  RS.registerCommand(module, "load-language", Packages.com.google.refine.commands.lang.LoadLanguageCommand());
  
  RS.registerCommand(module, "add-column", new Packages.com.google.refine.commands.column.AddColumnCommand());
  RS.registerCommand(module, "add-column-by-fetching-urls", new Packages.com.google.refine.commands.column.AddColumnByFetchingURLsCommand());
  RS.registerCommand(module, "remove-column", new Packages.com.google.refine.commands.column.RemoveColumnCommand());
  RS.registerCommand(module, "rename-column", new Packages.com.google.refine.commands.column.RenameColumnCommand());
  RS.registerCommand(module, "move-column", new Packages.com.google.refine.commands.column.MoveColumnCommand());
  RS.registerCommand(module, "split-column", new Packages.com.google.refine.commands.column.SplitColumnCommand());
  RS.registerCommand(module, "reorder-columns", new Packages.com.google.refine.commands.column.ReorderColumnsCommand());

  RS.registerCommand(module, "denormalize", new Packages.com.google.refine.commands.row.DenormalizeCommand());

  RS.registerCommand(module, "reconcile", new Packages.com.google.refine.commands.recon.ReconcileCommand());
  RS.registerCommand(module, "recon-match-best-candidates", new Packages.com.google.refine.commands.recon.ReconMatchBestCandidatesCommand());
  RS.registerCommand(module, "recon-mark-new-topics", new Packages.com.google.refine.commands.recon.ReconMarkNewTopicsCommand());
  RS.registerCommand(module, "recon-discard-judgments", new Packages.com.google.refine.commands.recon.ReconDiscardJudgmentsCommand());
  RS.registerCommand(module, "recon-match-specific-topic-to-cells", new Packages.com.google.refine.commands.recon.ReconMatchSpecificTopicCommand());
  RS.registerCommand(module, "recon-judge-one-cell", new Packages.com.google.refine.commands.recon.ReconJudgeOneCellCommand());
  RS.registerCommand(module, "recon-judge-similar-cells", new Packages.com.google.refine.commands.recon.ReconJudgeSimilarCellsCommand());
  RS.registerCommand(module, "recon-clear-one-cell", new Packages.com.google.refine.commands.recon.ReconClearOneCellCommand());
  RS.registerCommand(module, "recon-clear-similar-cells", new Packages.com.google.refine.commands.recon.ReconClearSimilarCellsCommand());
  RS.registerCommand(module, "recon-copy-across-columns", new Packages.com.google.refine.commands.recon.ReconCopyAcrossColumnsCommand());
  RS.registerCommand(module, "recon-use-values-as-identifiers", new Packages.com.google.refine.commands.recon.ReconUseValuesAsIdentifiersCommand());
  RS.registerCommand(module, "preview-extend-data", new Packages.com.google.refine.commands.recon.PreviewExtendDataCommand());
  RS.registerCommand(module, "extend-data", new Packages.com.google.refine.commands.recon.ExtendDataCommand());

  RS.registerCommand(module, "guess-types-of-column", new Packages.com.google.refine.commands.recon.GuessTypesOfColumnCommand());

  RS.registerCommand(module, "annotate-one-row", new Packages.com.google.refine.commands.row.AnnotateOneRowCommand());
  RS.registerCommand(module, "annotate-rows", new Packages.com.google.refine.commands.row.AnnotateRowsCommand());
  RS.registerCommand(module, "remove-rows", new Packages.com.google.refine.commands.row.RemoveRowsCommand());
  RS.registerCommand(module, "reorder-rows", new Packages.com.google.refine.commands.row.ReorderRowsCommand());

  RS.registerCommand(module, "get-expression-language-info", new Packages.com.google.refine.commands.expr.GetExpressionLanguageInfoCommand());
  RS.registerCommand(module, "get-expression-history", new Packages.com.google.refine.commands.expr.GetExpressionHistoryCommand());
  RS.registerCommand(module, "get-starred-expressions", new Packages.com.google.refine.commands.expr.GetStarredExpressionsCommand());     
  RS.registerCommand(module, "toggle-starred-expression", new Packages.com.google.refine.commands.expr.ToggleStarredExpressionCommand());
  RS.registerCommand(module, "log-expression", new Packages.com.google.refine.commands.expr.LogExpressionCommand());
  RS.registerCommand(module, "preview-expression", new Packages.com.google.refine.commands.expr.PreviewExpressionCommand());

  RS.registerCommand(module, "get-preference", new Packages.com.google.refine.commands.GetPreferenceCommand());
  RS.registerCommand(module, "get-all-preferences", new Packages.com.google.refine.commands.GetAllPreferencesCommand());
  RS.registerCommand(module, "set-preference", new Packages.com.google.refine.commands.SetPreferenceCommand());
  RS.registerCommand(module, "open-workspace-dir", new Packages.com.google.refine.commands.OpenWorkspaceDirCommand());
  
}

function registerOperations() {
  var OR = Packages.com.google.refine.operations.OperationRegistry;

  OR.registerOperation(module, "text-transform", Packages.com.google.refine.operations.cell.TextTransformOperation);
  OR.registerOperation(module, "mass-edit", Packages.com.google.refine.operations.cell.MassEditOperation);

  OR.registerOperation(module, "multivalued-cell-join", Packages.com.google.refine.operations.cell.MultiValuedCellJoinOperation);
  OR.registerOperation(module, "multivalued-cell-split", Packages.com.google.refine.operations.cell.MultiValuedCellSplitOperation);
  OR.registerOperation(module, "fill-down", Packages.com.google.refine.operations.cell.FillDownOperation);
  OR.registerOperation(module, "blank-down", Packages.com.google.refine.operations.cell.BlankDownOperation);
  OR.registerOperation(module, "transpose-columns-into-rows", Packages.com.google.refine.operations.cell.TransposeColumnsIntoRowsOperation);
  OR.registerOperation(module, "transpose-rows-into-columns", Packages.com.google.refine.operations.cell.TransposeRowsIntoColumnsOperation);
  OR.registerOperation(module, "key-value-columnize", Packages.com.google.refine.operations.cell.KeyValueColumnizeOperation);

  OR.registerOperation(module, "column-addition", Packages.com.google.refine.operations.column.ColumnAdditionOperation);
  OR.registerOperation(module, "column-removal", Packages.com.google.refine.operations.column.ColumnRemovalOperation);
  OR.registerOperation(module, "column-rename", Packages.com.google.refine.operations.column.ColumnRenameOperation);
  OR.registerOperation(module, "column-move", Packages.com.google.refine.operations.column.ColumnMoveOperation);
  OR.registerOperation(module, "column-split", Packages.com.google.refine.operations.column.ColumnSplitOperation);
  OR.registerOperation(module, "column-addition-by-fetching-urls", Packages.com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation);
  OR.registerOperation(module, "column-reorder", Packages.com.google.refine.operations.column.ColumnReorderOperation);

  OR.registerOperation(module, "row-removal", Packages.com.google.refine.operations.row.RowRemovalOperation);
  OR.registerOperation(module, "row-star", Packages.com.google.refine.operations.row.RowStarOperation);
  OR.registerOperation(module, "row-flag", Packages.com.google.refine.operations.row.RowFlagOperation);
  OR.registerOperation(module, "row-reorder", Packages.com.google.refine.operations.row.RowReorderOperation);

  OR.registerOperation(module, "recon", Packages.com.google.refine.operations.recon.ReconOperation);
  OR.registerOperation(module, "recon-mark-new-topics", Packages.com.google.refine.operations.recon.ReconMarkNewTopicsOperation);
  OR.registerOperation(module, "recon-match-best-candidates", Packages.com.google.refine.operations.recon.ReconMatchBestCandidatesOperation);
  OR.registerOperation(module, "recon-discard-judgments", Packages.com.google.refine.operations.recon.ReconDiscardJudgmentsOperation);
  OR.registerOperation(module, "recon-match-specific-topic-to-cells", Packages.com.google.refine.operations.recon.ReconMatchSpecificTopicOperation);
  OR.registerOperation(module, "recon-judge-similar-cells", Packages.com.google.refine.operations.recon.ReconJudgeSimilarCellsOperation);
  OR.registerOperation(module, "recon-clear-similar-cells", Packages.com.google.refine.operations.recon.ReconClearSimilarCellsOperation);
  OR.registerOperation(module, "recon-copy-across-columns", Packages.com.google.refine.operations.recon.ReconCopyAcrossColumnsOperation);
  OR.registerOperation(module, "extend-reconciled-data", Packages.com.google.refine.operations.recon.ExtendDataOperation);
  OR.registerOperation(module, "recon-use-values-as-identifiers", Packages.com.google.refine.operations.recon.ReconUseValuesAsIdentifiersOperation);
}

function registerImporting() {
  var IM = Packages.com.google.refine.importing.ImportingManager;

  /*
   *  Formats and their UI class names and parsers:
   *  - UI class names are used on the client-side in Javascript to instantiate code that lets the user
   *    configure the parser's options.
   *  - Parsers are server-side code that do the actual parsing. Because they have access to the raw files,
   *    they also generate defaults for the client-side UIs to initialize.
   */

  IM.registerFormat("text", "core-import-formats/text" ); // generic format, no parser to handle it
  IM.registerFormat("text/line-based", "core-import-formats/text/line-based", "LineBasedParserUI",
      new Packages.com.google.refine.importers.LineBasedImporter());
  IM.registerFormat("text/line-based/*sv", "core-import-formats/text/line-based/*sv", "SeparatorBasedParserUI",
      new Packages.com.google.refine.importers.SeparatorBasedImporter());
  IM.registerFormat("text/line-based/fixed-width", "core-import-formats/text/line-based/fixed-width", "FixedWidthParserUI",
      new Packages.com.google.refine.importers.FixedWidthImporter());

  IM.registerFormat("text/rdf/nt", "core-import-formats/text/rdf/nt", "RdfTriplesParserUI", 
              new Packages.com.google.refine.importers.RdfTripleImporter(Packages.com.google.refine.importers.RdfTripleImporter.Mode.NT));
  IM.registerFormat("text/rdf/n3", "core-import-formats/text/rdf/n3", "RdfTriplesParserUI", 
          new Packages.com.google.refine.importers.RdfTripleImporter(Packages.com.google.refine.importers.RdfTripleImporter.Mode.N3));
  IM.registerFormat("text/rdf/ttl", "core-import-formats/text/rdf/ttl", "RdfTriplesParserUI", 
                  new Packages.com.google.refine.importers.RdfTripleImporter(Packages.com.google.refine.importers.RdfTripleImporter.Mode.TTL));
  IM.registerFormat("text/rdf/xml", "core-import-formats/text/rdf/xml", "RdfTriplesParserUI", new Packages.com.google.refine.importers.RdfXmlTripleImporter());
  IM.registerFormat("text/rdf/ld+json", "core-import-formats/text/rdf/ld+json", "RdfTriplesParserUI", new Packages.com.google.refine.importers.RdfJsonldTripleImporter());

  IM.registerFormat("text/xml", "core-import-formats/text/xml", "XmlParserUI", new Packages.com.google.refine.importers.XmlImporter());
  IM.registerFormat("binary/text/xml/xls/xlsx", "core-import-formats/binary/text/xml/xls/xlsx", "ExcelParserUI", new Packages.com.google.refine.importers.ExcelImporter());
  IM.registerFormat("text/xml/ods", "core-import-formats/text/xml/ods", "ExcelParserUI", new Packages.com.google.refine.importers.OdsImporter());
  IM.registerFormat("text/json", "core-import-formats/text/json", "JsonParserUI", new Packages.com.google.refine.importers.JsonImporter());
  IM.registerFormat("text/marc", "core-import-formats/text/marc", "XmlParserUI", new Packages.com.google.refine.importers.MarcImporter());
  IM.registerFormat("text/wiki", "core-import-formats/text/wiki", "WikitextParserUI", new Packages.com.google.refine.importers.WikitextImporter());

  IM.registerFormat("binary", "core-import-formats/binary"); // generic format, no parser to handle it

  IM.registerFormat("service", "core-import-formats/service"); // generic format, no parser to handle it

  /*
   *  Extension to format mappings
   */
  IM.registerExtension(".txt", "text");
  IM.registerExtension(".csv", "text/line-based/*sv");
  IM.registerExtension(".tsv", "text/line-based/*sv");

  IM.registerExtension(".xml", "text/xml");
  IM.registerExtension(".atom", "text/xml");
  
  IM.registerExtension(".json", "text/json");
  IM.registerExtension(".js", "text/json");

  IM.registerExtension(".xls", "binary/text/xml/xls/xlsx");
  IM.registerExtension(".xlsx", "binary/text/xml/xls/xlsx");

  IM.registerExtension(".ods", "text/xml/ods");
  
  IM.registerExtension(".nt", "text/rdf/nt");
  IM.registerExtension(".ntriples", "text/rdf/nt");
  
  IM.registerExtension(".n3", "text/rdf/n3");
  IM.registerExtension(".ttl", "text/rdf/ttl");
  IM.registerExtension(".jsonld", "text/rdf/ld+json");
  IM.registerExtension(".rdf", "text/rdf/xml");

  IM.registerExtension(".marc", "text/marc");
  IM.registerExtension(".mrc", "text/marc");

  IM.registerExtension(".wiki", "text/wiki");

  /*
   *  Mime type to format mappings
   */
  IM.registerMimeType("text/plain", "text");
  IM.registerMimeType("text/csv", "text/line-based/*sv");
  IM.registerMimeType("text/x-csv", "text/line-based/*sv");
  IM.registerMimeType("text/tab-separated-value", "text/line-based/*sv");
  IM.registerMimeType("text/tab-separated-values", "text/line-based/*sv");

  IM.registerMimeType("text/fixed-width", "text/line-based/fixed-width");
  
  IM.registerMimeType("application/n-triples", "text/rdf/nt");
  IM.registerMimeType("text/n3", "text/rdf/n3");
  IM.registerMimeType("text/rdf+n3", "text/rdf/n3");
  IM.registerMimeType("text/turtle", "text/rdf/ttl");
  IM.registerMimeType("application/xml", "text/xml");
  IM.registerMimeType("text/xml", "text/xml");
  IM.registerMimeType("+xml", "text/xml"); // suffix will be tried only as fallback
  IM.registerMimeType("application/rdf+xml", "text/rdf/xml");
  IM.registerMimeType("application/ld+json", "text/rdf/ld+json");
  IM.registerMimeType("application/atom+xml", "text/xml");

  IM.registerMimeType("application/msexcel", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/x-msexcel", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/x-ms-excel", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/vnd.ms-excel", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/x-excel", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/xls", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/x-xls", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "binary/text/xml/xls/xlsx");
  IM.registerMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "binary/text/xml/xls/xlsx");

  IM.registerMimeType("application/vnd.oasis.opendocument.spreadsheet","text/xml/ods");

  IM.registerMimeType("application/json", "text/json");
  IM.registerMimeType("application/javascript", "text/json");
  IM.registerMimeType("text/json", "text/json");
  IM.registerMimeType("+json", "text/json"); // suffix will be tried only as fallback

  IM.registerMimeType("application/marc", "text/marc");
  
  /*
   *  Format guessers: these take a format derived from extensions or mime-types,
   *  look at the actual files' content, and try to guess a better format.
   */
  IM.registerFormatGuesser("text", new Packages.com.google.refine.importers.TextFormatGuesser());
  IM.registerFormatGuesser("text/line-based", new Packages.com.google.refine.importers.LineBasedFormatGuesser());

  /*
   *  Controllers: these implement high-level UI flows for importing data. For example, the default
   *  controller lets the user specify one or more source files, either local or remote or on the clipboard,
   *  lets the user select which files to actually import in case any of the original file is an archive
   *  containing several files, and then lets the user configure parsing options.
   */
  IM.registerController(
    module,
    "default-importing-controller",
    new Packages.com.google.refine.importing.DefaultImportingController()
  );
}

/*
 *  This optional function is invoked from the module's init() Java function.
 */
function init() {
  // Packages.java.lang.System.err.println("Initializing by script " + module);

  registerCommands();
  registerOperations();
  registerImporting();

  var commonModules = [
      "3rdparty/jquery.js",
      "3rdparty/jquery-migrate.js",
      "externals/jquery-ui/jquery-ui.js",
      "3rdparty/js.cookie.js",
      "3rdparty/underscore.js",

      "3rdparty/jquery.i18n/CLDRPluralRuleParser.js",
      "3rdparty/jquery.i18n/jquery.i18n.js",
      "3rdparty/jquery.i18n/jquery.i18n.messagestore.js",
      "3rdparty/jquery.i18n/jquery.i18n.fallbacks.js",
      "3rdparty/jquery.i18n/jquery.i18n.parser.js",
      "3rdparty/jquery.i18n/jquery.i18n.emitter.js",
      "3rdparty/jquery.i18n/jquery.i18n.language.js",
      "3rdparty/jquery.i18n/languages/fi.js",
      "3rdparty/jquery.i18n/languages/ru.js",
    ];

  var RC = Packages.com.google.refine.model.recon.ReconConfig;
  RC.registerReconConfig(module, "standard-service", Packages.com.google.refine.model.recon.StandardReconConfig);

  ClientSideResourceManager.addPaths(
    "index/scripts",
    module,
    commonModules.concat([
      "3rdparty/tablesorter/jquery.tablesorter.js",
      "3rdparty/select2/select2.js",

      "scripts/util/misc.js",
      "scripts/util/url.js",
      "scripts/util/string.js",
      "scripts/util/ajax.js",
      "scripts/util/i18n.js",
      "scripts/util/csrf.js",
      "scripts/util/menu.js",
      "scripts/util/dialog.js",
      "scripts/util/dom.js",
      "scripts/util/encoding.js",
      "scripts/util/sign.js",

      "scripts/index.js",
      "scripts/index/create-project-ui.js",
      "scripts/index/open-project-ui.js",
      "scripts/index/import-project-ui.js",
      "scripts/index/lang-settings-ui.js",

      "scripts/index/default-importing-controller/controller.js",
      "scripts/index/default-importing-controller/file-selection-panel.js",
      "scripts/index/default-importing-controller/parsing-panel.js",

      "scripts/index/default-importing-sources/sources.js",
      "scripts/index/parser-interfaces/preview-table.js",
      "scripts/index/parser-interfaces/separator-based-parser-ui.js",
      "scripts/index/parser-interfaces/line-based-parser-ui.js",
      "scripts/index/parser-interfaces/fixed-width-parser-ui.js",
      "scripts/index/parser-interfaces/excel-parser-ui.js",
      "scripts/index/parser-interfaces/xml-parser-ui.js",
      "scripts/index/parser-interfaces/json-parser-ui.js",
      "scripts/index/parser-interfaces/rdf-triples-parser-ui.js",
      "scripts/index/parser-interfaces/wikitext-parser-ui.js",

      "scripts/reconciliation/recon-manager.js", // so that reconciliation functions are available to importers
      "scripts/index/edit-metadata-dialog.js"
    ])
  );

  ClientSideResourceManager.addPaths(
    "index/styles",
    module,
    [
      "externals/jquery-ui/css/ui-lightness/jquery-ui.css",
      "3rdparty/select2/select2.css",
      "3rdparty/tablesorter/theme.blue.css",

      "styles/theme.css",
      "styles/jquery-ui-overrides.css",
      "styles/common.css",
      "styles/util/dialog.css",
      "styles/util/encoding.css",
      
      "styles/index.css",
      "styles/index/create-project-ui.css",
      "styles/index/open-project-ui.css",
      "styles/index/import-project-ui.css",

      "styles/index/default-importing-controller.css",
      "styles/index/default-importing-file-selection-panel.css",
      "styles/index/default-importing-parsing-panel.css",

      "styles/index/default-importing-sources.css",
      "styles/views/data-table-view.css", // for the preview table's styles
      "styles/index/fixed-width-parser-ui.css",
      "styles/index/xml-parser-ui.css",
      "styles/index/json-parser-ui.css",
      "styles/index/wikitext-parser-ui.css",
    ]
  );

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    commonModules.concat([
      "externals/suggest/suggest-4_3a.js",
      "scripts/util/i18n.js",
      "scripts/util/csrf.js",
      "scripts/project.js",
      "scripts/util/misc.js",
      "scripts/util/url.js",
      "scripts/util/string.js",
      "scripts/util/ajax.js",
      "scripts/util/menu.js",
      "scripts/util/dialog.js",
      "scripts/util/dom.js",
      "scripts/util/custom-suggest.js",
      "scripts/util/encoding.js",
      "scripts/util/sign.js",

      "scripts/widgets/histogram-widget.js",
      "scripts/widgets/slider-widget.js",

      "scripts/project/browsing-engine.js",
      "scripts/project/history-panel.js",
      "scripts/project/process-panel.js",
      "scripts/project/extension-bar.js",
      "scripts/project/summary-bar.js",
      "scripts/project/exporters.js",
      "scripts/project/scripting.js",

      "scripts/facets/facet.js",
      "scripts/facets/list-facet.js",
      "scripts/facets/range-facet.js",
      "scripts/facets/timerange-facet.js",
      "externals/imgareaselect/jquery.imgareaselect.js", // Used by scatterplot facet only
      "scripts/facets/scatterplot-facet.js",
      "scripts/facets/text-search-facet.js",

      "scripts/views/data-table/cell-renderers/null-renderer.js",
      "scripts/views/data-table/cell-renderers/error-renderer.js",
      "scripts/views/data-table/cell-renderers/simple-value-renderer.js",
      "scripts/views/data-table/cell-renderers/recon-renderer.js",
      "scripts/views/data-table/cell-renderers/registry.js",

      "scripts/views/data-table/data-table-view.js",
      "scripts/views/data-table/cell-ui.js",
      "scripts/views/data-table/column-header-ui.js",
      "scripts/views/data-table/menu-facets.js",
      "scripts/views/data-table/menu-edit-cells.js",
      "scripts/views/data-table/menu-edit-column.js",
      "scripts/views/data-table/menu-reconcile.js",

      "scripts/reconciliation/recon-manager.js",
      "scripts/reconciliation/recon-dialog.js",
      "scripts/reconciliation/recon-dialog-2.js",
      "scripts/reconciliation/standard-service-panel.js",

      "scripts/dialogs/expression-preview-dialog.js",
      "scripts/dialogs/add-column-by-reconciliation.js",
      "scripts/dialogs/clustering-dialog.js",
      "scripts/dialogs/scatterplot-dialog.js",
      "scripts/dialogs/templating-exporter-dialog.js",
      "scripts/dialogs/column-reordering-dialog.js",
      "scripts/dialogs/common-transform-dialog.js",
      "scripts/dialogs/custom-tabular-exporter-dialog.js",
      "scripts/dialogs/sql-exporter-dialog.js",
      "scripts/dialogs/expression-column-dialog.js",
      "scripts/dialogs/http-headers-dialog.js",
    ])
  );

  ClientSideResourceManager.addPaths(
    "project/styles",
    module,
    [
      "externals/suggest/css/suggest-4_3.css",
      "externals/jquery-ui/css/ui-lightness/jquery-ui.css",
      "externals/imgareaselect/css/imgareaselect-default.css",

      "styles/theme.css",
      "styles/jquery-ui-overrides.css",
      "styles/common.css",

      "styles/util/menu.css",
      "styles/util/dialog.css",
      "styles/util/custom-suggest.css",
      "styles/util/encoding.css",

      "styles/project.css",
      "styles/project/sidebar.css",
      "styles/project/facets.css",
      "styles/project/process.css",

      "styles/widgets/histogram-widget.css",
      "styles/widgets/slider-widget.css",

      "styles/views/data-table-view.css",
      "styles/views/column-join.css",

      "styles/dialogs/expression-preview-dialog.css",
      "styles/dialogs/clustering-dialog.css",
      "styles/dialogs/scatterplot-dialog.css",
      "styles/dialogs/column-reordering-dialog.css",
      "styles/dialogs/custom-tabular-exporter-dialog.css",
      "styles/dialogs/sql-exporter-dialog.css",
      "styles/dialogs/recon-service-selection-dialog.css",
      "styles/reconciliation/recon-dialog.css",
      "styles/reconciliation/standard-service-panel.css",
      "styles/reconciliation/add-column-by-reconciliation.css",
    ]
  );

  ClientSideResourceManager.addPaths(
    "preferences/scripts",
    module,
    commonModules.concat([
      "scripts/util/i18n.js",
      "scripts/util/csrf.js",
      "scripts/preferences.js",
    ])
  );
  ClientSideResourceManager.addPaths(
    "preferences/styles",
    module,
    [
      "externals/suggest/css/suggest-4_3.css",
      "externals/jquery-ui/css/ui-lightness/jquery-ui.css",

      "styles/theme.css",
      "styles/jquery-ui-overrides.css",
      "styles/common.css",
      "styles/util/dialog.css"
    ]
  );
}

/*
 * This is the function that is invoked by Butterfly
 */
function process(path, request, response) {
  if (path == "wirings.js") {
    var wirings = butterfly.getWirings(request);
    butterfly.sendString(
      request, 
      response, 
      "var ModuleWirings = " + butterfly.toJSONString(wirings) + ";", 
      encoding, 
      "text/javascript"
    );
  } else {
    if (path == "/" || path == "") {
      path = "index";
    } else if (path.endsWith("/")) {
      path = path.substring(0, path.length - 1);
    }

    var slash = path.lastIndexOf("/");
    var lastSegment = slash >= 0 ? path.substring(slash + 1) : path;

    if (path.endsWith("-bundle.js")) {
      lastSegment = lastSegment.substring(0, lastSegment.length - "-bundle.js".length);

      response.setContentType("text/javascript");
      response.setCharacterEncoding(encoding);

      var output = response.getWriter();
      try {
        var paths = ClientSideResourceManager.getPaths(lastSegment + "/scripts");
        for (var key in paths) {
          if (!paths.hasOwnProperty(key)) {
            continue;
          }
          var qualifiedPath = paths[key];
          var input = null;
          try {
            var url = qualifiedPath.module.getResource(qualifiedPath.path);
            var urlConnection = url.openConnection();

            input = new Packages.java.io.BufferedReader(
                new Packages.java.io.InputStreamReader(urlConnection.getInputStream(), "UTF8"));

            output.write("/* ===== "); 
            output.write(qualifiedPath.fullPath); 
            output.write(" ===== */\n\n");

            Packages.org.apache.commons.io.IOUtils.copy(input, output);

            output.write("\n\n");
          } catch (e) {
            // silent
          } finally {
            if (input != null) input.close();
          }
        }
      } catch (e) {
        // silent
      } finally {
        butterfly.responded();
      }
      return true;
    } else {
      if (lastSegment in templatedFiles) {
        var context = {};

        var params = new Packages.java.util.Properties();
        var e = request.getParameterNames();
        while (e.hasMoreElements()) {
          var name = e.nextElement();
          params.put(name, request.getParameterValues(name)[0]);
        }
        context.params = params;
        
        // We prepend '' to convert the Java string to a Javascript string.
        context.projectID = ('' + request.getParameter("project")).replace(/\D/g, '');
        
        var styles = ClientSideResourceManager.getPaths(lastSegment + "/styles");
        var styleInjection = [];
        for (var key in styles) {
          if (styles.hasOwnProperty(key)) {
            var qualifiedPath = styles[key];
            styleInjection.push(
                '<link type="text/css" rel="stylesheet" href="' + qualifiedPath.fullPath.substring(1) + '" />');
          }
        }
        context.styleInjection = styleInjection.join("\n");

        if (bundle) {
          context.scriptInjection = '<script type="text/javascript" src="' + path + '-bundle.js"></script>';
        } else {
          var scripts = ClientSideResourceManager.getPaths(lastSegment + "/scripts");
          var scriptInjection = [];
          for (var key in scripts) {
            if (scripts.hasOwnProperty(key)) {
              var qualifiedPath = scripts[key];
              scriptInjection.push(
                  '<script type="text/javascript" src="' + qualifiedPath.fullPath.substring(1) + '"></script>');
            }
          }
          context.scriptInjection = scriptInjection.join("\n");
        }
        
        if (templatedFiles[lastSegment].outputEncodings) {
          var encodings = [];
          
          var sortedCharsetMap = Packages.java.nio.charset.Charset.availableCharsets();
          var keySetArray = sortedCharsetMap.keySet().toArray();
          for (var key in keySetArray) {
            if (!keySetArray.hasOwnProperty(key)) {
              continue;
            }
            var code = keySetArray[key];
            var charset = sortedCharsetMap.get(code);
            var aliasesArray = charset.aliases().toArray();
            var aliases = [];
            for (var key1 in aliasesArray) {
              if (aliasesArray.hasOwnProperty(key1)) {
                aliases.push(aliasesArray[key1]);
              }
            }
            
            encodings.push({
              code: code,
              name: charset.displayName(),
              aliases: aliases
            });
          }
          
          context.encodingJson = butterfly.toJSONString(encodings);
          context.defaultEncoding = butterfly.toJSONString(Packages.java.nio.charset.Charset.defaultCharset().name());
        }
        
        send(request, response, path + ".vt", context);
      }
    }
  }
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
