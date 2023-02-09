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
var ClientSideResourceManager = Packages.org.openrefine.ClientSideResourceManager;
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
  var RS = Packages.org.openrefine.RefineServlet;

  RS.registerCommand(module, "get-version", new Packages.org.openrefine.commands.GetVersionCommand());
  RS.registerCommand(module, "get-csrf-token", new Packages.org.openrefine.commands.GetCSRFTokenCommand());

  RS.registerCommand(module, "get-importing-configuration", new Packages.org.openrefine.commands.importing.GetImportingConfigurationCommand());
  RS.registerCommand(module, "create-importing-job", new Packages.org.openrefine.commands.importing.CreateImportingJobCommand());
  RS.registerCommand(module, "get-importing-job-status", new Packages.org.openrefine.commands.importing.GetImportingJobStatusCommand());
  RS.registerCommand(module, "importing-controller", new Packages.org.openrefine.commands.importing.ImportingControllerCommand());
  RS.registerCommand(module, "cancel-importing-job", new Packages.org.openrefine.commands.importing.CancelImportingJobCommand());

  RS.registerCommand(module, "create-project-from-upload", new Packages.org.openrefine.commands.project.CreateProjectCommand());
  RS.registerCommand(module, "import-project", new Packages.org.openrefine.commands.project.ImportProjectCommand());
  RS.registerCommand(module, "export-project", new Packages.org.openrefine.commands.project.ExportProjectCommand());
  RS.registerCommand(module, "export-rows", new Packages.org.openrefine.commands.project.ExportRowsCommand());

  RS.registerCommand(module, "get-project-metadata", new Packages.org.openrefine.commands.project.GetProjectMetadataCommand());
  RS.registerCommand(module, "get-all-project-metadata", new Packages.org.openrefine.commands.workspace.GetAllProjectMetadataCommand());
  RS.registerCommand(module, "set-project-metadata", new Packages.org.openrefine.commands.project.SetProjectMetadataCommand());
  RS.registerCommand(module, "get-all-project-tags", new Packages.org.openrefine.commands.workspace.GetAllProjectTagsCommand());
  RS.registerCommand(module, "set-project-tags", new Packages.org.openrefine.commands.project.SetProjectTagsCommand());

  RS.registerCommand(module, "delete-project", new Packages.org.openrefine.commands.project.DeleteProjectCommand());
  RS.registerCommand(module, "rename-project", new Packages.org.openrefine.commands.project.RenameProjectCommand());

  RS.registerCommand(module, "get-models", new Packages.org.openrefine.commands.project.GetModelsCommand());
  RS.registerCommand(module, "get-rows", new Packages.org.openrefine.commands.row.GetRowsCommand());
  RS.registerCommand(module, "get-processes", new Packages.org.openrefine.commands.history.GetProcessesCommand());
  RS.registerCommand(module, "get-history", new Packages.org.openrefine.commands.history.GetHistoryCommand());
  RS.registerCommand(module, "get-operations", new Packages.org.openrefine.commands.history.GetOperationsCommand());
  RS.registerCommand(module, "get-columns-info", new Packages.org.openrefine.commands.column.GetColumnsInfoCommand());
  RS.registerCommand(module, "get-scatterplot", new Packages.org.openrefine.commands.browsing.GetScatterplotCommand());

  RS.registerCommand(module, "undo-redo", new Packages.org.openrefine.commands.history.UndoRedoCommand());
  RS.registerCommand(module, "apply-operations", new Packages.org.openrefine.commands.history.ApplyOperationsCommand());
  RS.registerCommand(module, "cancel-processes", new Packages.org.openrefine.commands.history.CancelProcessesCommand());

  RS.registerCommand(module, "compute-facets", new Packages.org.openrefine.commands.browsing.ComputeFacetsCommand());
  RS.registerCommand(module, "compute-clusters", new Packages.org.openrefine.commands.browsing.ComputeClustersCommand());
  RS.registerCommand(module, "get-clustering-functions-and-distances", new Packages.org.openrefine.commands.browsing.GetClusteringFunctionsAndDistancesCommand());

  RS.registerCommand(module, "edit-one-cell", new Packages.org.openrefine.commands.cell.EditOneCellCommand());

  RS.registerCommand(module, "get-languages", Packages.org.openrefine.commands.lang.GetLanguagesCommand());
  RS.registerCommand(module, "load-language", Packages.org.openrefine.commands.lang.LoadLanguageCommand());
  
  RS.registerCommand(module, "recon-judge-one-cell", new Packages.org.openrefine.commands.recon.ReconJudgeOneCellCommand());
  RS.registerCommand(module, "recon-clear-one-cell", new Packages.org.openrefine.commands.recon.ReconClearOneCellCommand());

  RS.registerCommand(module, "preview-extend-data", new Packages.org.openrefine.commands.recon.PreviewExtendDataCommand());

  RS.registerCommand(module, "guess-types-of-column", new Packages.org.openrefine.commands.recon.GuessTypesOfColumnCommand());

  RS.registerCommand(module, "annotate-one-row", new Packages.org.openrefine.commands.row.AnnotateOneRowCommand());

  RS.registerCommand(module, "get-expression-language-info", new Packages.org.openrefine.commands.expr.GetExpressionLanguageInfoCommand());
  RS.registerCommand(module, "get-expression-history", new Packages.org.openrefine.commands.expr.GetExpressionHistoryCommand());
  RS.registerCommand(module, "get-starred-expressions", new Packages.org.openrefine.commands.expr.GetStarredExpressionsCommand());     
  RS.registerCommand(module, "toggle-starred-expression", new Packages.org.openrefine.commands.expr.ToggleStarredExpressionCommand());
  RS.registerCommand(module, "log-expression", new Packages.org.openrefine.commands.expr.LogExpressionCommand());
  RS.registerCommand(module, "preview-expression", new Packages.org.openrefine.commands.expr.PreviewExpressionCommand());

  RS.registerCommand(module, "get-preference", new Packages.org.openrefine.commands.GetPreferenceCommand());
  RS.registerCommand(module, "get-all-preferences", new Packages.org.openrefine.commands.GetAllPreferencesCommand());
  RS.registerCommand(module, "set-preference", new Packages.org.openrefine.commands.SetPreferenceCommand());
  RS.registerCommand(module, "open-workspace-dir", new Packages.org.openrefine.commands.OpenWorkspaceDirCommand());
}

function registerOperations() {
  var OR = Packages.org.openrefine.operations.OperationRegistry;

  OR.registerOperation(module.getName(), "text-transform", Packages.org.openrefine.operations.cell.TextTransformOperation);
  OR.registerOperation(module.getName(), "mass-edit", Packages.org.openrefine.operations.cell.MassEditOperation);

  OR.registerOperation(module.getName(), "multivalued-cell-join", Packages.org.openrefine.operations.cell.MultiValuedCellJoinOperation);
  OR.registerOperation(module.getName(), "multivalued-cell-split", Packages.org.openrefine.operations.cell.MultiValuedCellSplitOperation);
  OR.registerOperation(module.getName(), "fill-down", Packages.org.openrefine.operations.cell.FillDownOperation);
  OR.registerOperation(module.getName(), "blank-down", Packages.org.openrefine.operations.cell.BlankDownOperation);
  OR.registerOperation(module.getName(), "transpose-columns-into-rows", Packages.org.openrefine.operations.cell.TransposeColumnsIntoRowsOperation);
  OR.registerOperation(module.getName(), "transpose-rows-into-columns", Packages.org.openrefine.operations.cell.TransposeRowsIntoColumnsOperation);
  OR.registerOperation(module.getName(), "key-value-columnize", Packages.org.openrefine.operations.cell.KeyValueColumnizeOperation);

  OR.registerOperation(module.getName(), "column-addition", Packages.org.openrefine.operations.column.ColumnAdditionOperation);
  OR.registerOperation(module.getName(), "column-removal", Packages.org.openrefine.operations.column.ColumnRemovalOperation);
  OR.registerOperation(module.getName(), "column-rename", Packages.org.openrefine.operations.column.ColumnRenameOperation);
  OR.registerOperation(module.getName(), "column-move", Packages.org.openrefine.operations.column.ColumnMoveOperation);
  OR.registerOperation(module.getName(), "column-split", Packages.org.openrefine.operations.column.ColumnSplitOperation);
  OR.registerOperation(module.getName(), "column-addition-by-fetching-urls", Packages.org.openrefine.operations.column.ColumnAdditionByFetchingURLsOperation);
  OR.registerOperation(module.getName(), "column-reorder", Packages.org.openrefine.operations.column.ColumnReorderOperation);

  OR.registerOperation(module.getName(), "row-removal", Packages.org.openrefine.operations.row.RowRemovalOperation);
  OR.registerOperation(module.getName(), "row-star", Packages.org.openrefine.operations.row.RowStarOperation);
  OR.registerOperation(module.getName(), "row-flag", Packages.org.openrefine.operations.row.RowFlagOperation);
  OR.registerOperation(module.getName(), "row-reorder", Packages.org.openrefine.operations.row.RowReorderOperation);

  OR.registerOperation(module.getName(), "recon", Packages.org.openrefine.operations.recon.ReconOperation);
  OR.registerOperation(module.getName(), "recon-mark-new-topics", Packages.org.openrefine.operations.recon.ReconMarkNewTopicsOperation);
  OR.registerOperation(module.getName(), "recon-match-best-candidates", Packages.org.openrefine.operations.recon.ReconMatchBestCandidatesOperation);
  OR.registerOperation(module.getName(), "recon-discard-judgments", Packages.org.openrefine.operations.recon.ReconDiscardJudgmentsOperation);
  OR.registerOperation(module.getName(), "recon-match-specific-topic-to-cells", Packages.org.openrefine.operations.recon.ReconMatchSpecificTopicOperation);
  OR.registerOperation(module.getName(), "recon-judge-similar-cells", Packages.org.openrefine.operations.recon.ReconJudgeSimilarCellsOperation);
  OR.registerOperation(module.getName(), "recon-clear-similar-cells", Packages.org.openrefine.operations.recon.ReconClearSimilarCellsOperation);
  OR.registerOperation(module.getName(), "recon-copy-across-columns", Packages.org.openrefine.operations.recon.ReconCopyAcrossColumnsOperation);
  OR.registerOperation(module.getName(), "extend-reconciled-data", Packages.org.openrefine.operations.recon.ExtendDataOperation);
  OR.registerOperation(module.getName(), "recon-use-values-as-identifiers", Packages.org.openrefine.operations.recon.ReconUseValuesAsIdentifiersOperation);
}

function registerImporting() {
  var FR = Packages.org.openrefine.importing.FormatRegistry;

  /*
   *  Formats and their UI class names and parsers:
   *  - UI class names are used on the client-side in Javascript to instantiate code that lets the user
   *    configure the parser's options.
   *  - Parsers are server-side code that do the actual parsing. Because they have access to the raw files,
   *    they also generate defaults for the client-side UIs to initialize.
   */

  FR.registerFormat("text", "core-import-formats/text" ); // generic format, no parser to handle it
  FR.registerFormat("text/line-based", "core-import-formats/text/line-based", "LineBasedParserUI",
      new Packages.org.openrefine.importers.LineBasedImporter());
  FR.registerFormat("text/line-based/*sv", "core-import-formats/text/line-based/*sv", "SeparatorBasedParserUI",
      new Packages.org.openrefine.importers.SeparatorBasedImporter());
  FR.registerFormat("text/line-based/fixed-width", "core-import-formats/text/line-based/fixed-width", "FixedWidthParserUI",
      new Packages.org.openrefine.importers.FixedWidthImporter());

  FR.registerFormat("text/rdf/nt", "core-import-formats/text/rdf/nt", "RdfTriplesParserUI", 
              new Packages.org.openrefine.importers.RdfTripleImporter(Packages.org.openrefine.importers.RdfTripleImporter.Mode.NT));
  FR.registerFormat("text/rdf/n3", "core-import-formats/text/rdf/n3", "RdfTriplesParserUI", 
          new Packages.org.openrefine.importers.RdfTripleImporter(Packages.org.openrefine.importers.RdfTripleImporter.Mode.N3));
  FR.registerFormat("text/rdf/ttl", "core-import-formats/text/rdf/ttl", "RdfTriplesParserUI", 
                  new Packages.org.openrefine.importers.RdfTripleImporter(Packages.org.openrefine.importers.RdfTripleImporter.Mode.TTL));
  FR.registerFormat("text/rdf/xml", "core-import-formats/text/rdf/xml", "RdfTriplesParserUI", new Packages.org.openrefine.importers.RdfXmlTripleImporter());
  FR.registerFormat("text/rdf/ld+json", "core-import-formats/text/rdf/ld+json", "RdfTriplesParserUI", new Packages.org.openrefine.importers.RdfJsonldTripleImporter());

  FR.registerFormat("text/xml", "core-import-formats/text/xml", "XmlParserUI", new Packages.org.openrefine.importers.XmlImporter());
  FR.registerFormat("binary/text/xml/xls/xlsx", "core-import-formats/binary/text/xml/xls/xlsx", "ExcelParserUI", new Packages.org.openrefine.importers.ExcelImporter());
  FR.registerFormat("text/xml/ods", "core-import-formats/text/xml/ods", "ExcelParserUI", new Packages.org.openrefine.importers.OdsImporter());
  FR.registerFormat("text/json", "core-import-formats/text/json", "JsonParserUI", new Packages.org.openrefine.importers.JsonImporter());
  FR.registerFormat("text/marc", "core-import-formats/text/marc", "XmlParserUI", new Packages.org.openrefine.importers.MarcImporter());
  FR.registerFormat("text/wiki", "core-import-formats/text/wiki", "WikitextParserUI", new Packages.org.openrefine.importers.WikitextImporter());
  FR.registerFormat("openrefine-legacy", null, null, new Packages.org.openrefine.importers.LegacyProjectImporter());

  FR.registerFormat("binary", "core-import-formats/binary"); // generic format, no parser to handle it

  FR.registerFormat("service", "core-import-formats/service"); // generic format, no parser to handle it

  /*
   *  Extension to format mappings
   */
  FR.registerExtension(".txt", "text");
  FR.registerExtension(".csv", "text/line-based/*sv");
  FR.registerExtension(".tsv", "text/line-based/*sv");

  FR.registerExtension(".xml", "text/xml");
  FR.registerExtension(".atom", "text/xml");
  
  FR.registerExtension(".json", "text/json");
  FR.registerExtension(".js", "text/json");

  FR.registerExtension(".xls", "binary/text/xml/xls/xlsx");
  FR.registerExtension(".xlsx", "binary/text/xml/xls/xlsx");

  FR.registerExtension(".ods", "text/xml/ods");
  
  FR.registerExtension(".n3", "text/rdf/n3");
  FR.registerExtension(".ttl", "text/rdf/ttl");
  FR.registerExtension(".jsonld", "text/rdf/ld+json");
  FR.registerExtension(".rdf", "text/rdf/xml");

  FR.registerExtension(".marc", "text/marc");
  FR.registerExtension(".mrc", "text/marc");

  FR.registerExtension(".wiki", "text/wiki");

  /*
   *  Mime type to format mappings
   */
  FR.registerMimeType("text/plain", "text");
  FR.registerMimeType("text/csv", "text/line-based/*sv");
  FR.registerMimeType("text/x-csv", "text/line-based/*sv");
  FR.registerMimeType("text/tab-separated-value", "text/line-based/*sv");
  FR.registerMimeType("text/tab-separated-values", "text/line-based/*sv");

  FR.registerMimeType("text/fixed-width", "text/line-based/fixed-width");
  
  FR.registerMimeType("application/n-triples", "text/rdf/nt");
  FR.registerMimeType("text/n3", "text/rdf/n3");
  FR.registerMimeType("text/rdf+n3", "text/rdf/n3");
  FR.registerMimeType("text/turtle", "text/rdf/ttl");
  FR.registerMimeType("application/xml", "text/xml");
  FR.registerMimeType("text/xml", "text/xml");
  FR.registerMimeType("+xml", "text/xml"); // suffix will be tried only as fallback
  FR.registerMimeType("application/rdf+xml", "text/rdf/xml");
  FR.registerMimeType("application/ld+json", "text/rdf/ld+json");
  FR.registerMimeType("application/atom+xml", "text/xml");

  FR.registerMimeType("application/msexcel", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/x-msexcel", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/x-ms-excel", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/vnd.ms-excel", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/x-excel", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/xls", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/x-xls", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "binary/text/xml/xls/xlsx");
  FR.registerMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "binary/text/xml/xls/xlsx");

  FR.registerMimeType("application/vnd.oasis.opendocument.spreadsheet","text/xml/ods");

  FR.registerMimeType("application/json", "text/json");
  FR.registerMimeType("application/javascript", "text/json");
  FR.registerMimeType("text/json", "text/json");
  FR.registerMimeType("+json", "text/json"); // suffix will be tried only as fallback

  FR.registerMimeType("application/marc", "text/marc");
  
  /*
   *  Format guessers: these take a format derived from extensions or mime-types,
   *  look at the actual files' content, and try to guess a better format.
   */
  FR.registerFormatGuesser("text", new Packages.org.openrefine.importers.TextFormatGuesser());
  FR.registerFormatGuesser("text/line-based", new Packages.org.openrefine.importers.LineBasedFormatGuesser());

  /*
   *  Controllers: these implement high-level UI flows for importing data. For example, the default
   *  controller lets the user specify one or more source files, either local or remote or on the clipboard,
   *  lets the user select which files to actually import in case any of the original file is an archive
   *  containing several files, and then lets the user configure parsing options.
   */
  var IM = Packages.org.openrefine.importing.ImportingManager;
  IM.registerController(
    module,
    "default-importing-controller",
    new Packages.org.openrefine.importing.DefaultImportingController()
  );
}

function registerLanguages() {
  var MP = Packages.org.openrefine.expr.MetaParser;
  MP.registerLanguageParser("grel", "General Refine Expression Language (GREL)", Packages.org.openrefine.grel.Parser.grelParser, "value");
}

function registerFacets() {
  var FCR = Packages.org.openrefine.browsing.facets.FacetConfigResolver;
  FCR.registerFacetConfig("core", "list", Packages.org.openrefine.browsing.facets.ListFacet.ListFacetConfig);
  FCR.registerFacetConfig("core", "range", Packages.org.openrefine.browsing.facets.RangeFacet.RangeFacetConfig);
  FCR.registerFacetConfig("core", "timerange", Packages.org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig);
  FCR.registerFacetConfig("core", "text", Packages.org.openrefine.browsing.facets.TextSearchFacet.TextSearchFacetConfig);
  FCR.registerFacetConfig("core", "scatterplot", Packages.org.openrefine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig);
}

function registerDistances() {
   var DF = Packages.org.openrefine.clustering.knn.DistanceFactory;
   var VicinoDistance = Packages.org.openrefine.clustering.knn.VicinoDistance;
   DF.put("levenshtein", new VicinoDistance(new Packages.edu.mit.simile.vicino.distances.LevenshteinDistance()));
   DF.put("ppm", new VicinoDistance(new Packages.edu.mit.simile.vicino.distances.PPMDistance()));
        
   // Distances not activated as they are not very useful:
   // See https://github.com/OpenRefine/OpenRefine/pull/1906
   /*
   DF.put("jaccard", new VicinoDistance(new JaccardDistance()));
   DF.put("jaro", new VicinoDistance(new JaroDistance()));
   DF.put("jaro-winkler", new VicinoDistance(new JaroWinklerDistance()));
   DF.put("jaro-winkler-tfidf", new VicinoDistance(new JaroWinklerTFIDFDistance()));
   DF.put("gzip", new VicinoDistance(new GZipDistance()));
   DF.put("bzip2", new VicinoDistance(new BZip2Distance()));
   */
}

function registerClusterers() {
   var CCF = Packages.org.openrefine.clustering.ClustererConfigFactory;
   CCF.register("knn", Packages.org.openrefine.clustering.knn.kNNClusterer.kNNClustererConfig);
   // Binning clusterer: already registered by default.
}

function registerReconConfigs() {
   var RC = Packages.org.openrefine.model.recon.ReconConfig;
   RC.registerReconConfig("core", "standard-service", Packages.org.openrefine.model.recon.StandardReconConfig);
}

/*
 *  This optional function is invoked from the module's init() Java function.
 */
function init() {
  // Packages.java.lang.System.err.println("Initializing by script " + module);

  registerLanguages();
  registerFacets();
  registerCommands();
  registerOperations();
  registerImporting();
  registerClusterers();
  registerDistances();
  registerReconConfigs();

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

  var RC = Packages.org.openrefine.model.recon.ReconConfig;
  RC.registerReconConfig(module, "standard-service", Packages.org.openrefine.model.recon.StandardReconConfig);

  ClientSideResourceManager.addPaths(
    "index/scripts",
    module,
    commonModules.concat([
      "3rdparty/date.js",
      "3rdparty/tablesorter/jquery.tablesorter.js",
      "3rdparty/moment-with-locales.js",
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
      "scripts/util/filter-lists.js",

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
      "styles/jquery-ui-overrides.less",
      "styles/common.less",
      "styles/pure.css",
      "styles/util/dialog.less",
      "styles/util/encoding.less",
      
      "styles/index.less",
      "styles/index/create-project-ui.less",
      "styles/index/open-project-ui.less",
      "styles/index/import-project-ui.less",

      "styles/index/default-importing-controller.less",
      "styles/index/default-importing-file-selection-panel.less",
      "styles/index/default-importing-parsing-panel.less",

      "styles/index/default-importing-sources.less",
      "styles/views/data-table-view.less", // for the preview table's styles
      "styles/index/fixed-width-parser-ui.less",
      "styles/index/xml-parser-ui.less",
      "styles/index/json-parser-ui.less",
      "styles/index/wikitext-parser-ui.less",
    ]
  );

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    commonModules.concat([
      "externals/suggest/suggest-4_3a.js",
      "3rdparty/date.js",
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

      "scripts/views/data-table/data-table-view.js",
      "scripts/views/data-table/cell-ui.js",
      "scripts/views/data-table/column-header-ui.js",
      "scripts/views/data-table/menu-facets.js",
      "scripts/views/data-table/menu-edit-cells.js",
      "scripts/views/data-table/menu-edit-column.js",
      "scripts/views/data-table/menu-reconcile.js",

      "scripts/reconciliation/recon-manager.js",
      "scripts/reconciliation/recon-dialog.js",
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

      "styles/jquery-ui-overrides.less",
      "styles/common.less",
      "styles/pure.css",

      "styles/util/menu.less",
      "styles/util/dialog.less",
      "styles/util/custom-suggest.less",
      "styles/util/encoding.less",

      "styles/project.less",
      "styles/project/sidebar.less",
      "styles/project/facets.less",
      "styles/project/process.less",

      "styles/widgets/histogram-widget.less",
      "styles/widgets/slider-widget.less",

      "styles/views/data-table-view.less",
      "styles/views/column-join.less",

      "styles/dialogs/expression-preview-dialog.less",
      "styles/dialogs/clustering-dialog.less",
      "styles/dialogs/scatterplot-dialog.less",
      "styles/dialogs/column-reordering-dialog.less",
      "styles/dialogs/custom-tabular-exporter-dialog.less",
      "styles/dialogs/sql-exporter-dialog.less",
      "styles/dialogs/recon-service-selection-dialog.less",
      "styles/dialogs/confirm-history-erasure-dialog.less",
      "styles/reconciliation/recon-dialog.less",
      "styles/reconciliation/standard-service-panel.less",
      "styles/reconciliation/add-column-by-reconciliation.less",
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
      "styles/jquery-ui-overrides.less",
      "styles/common.less",
      "styles/pure.css",
      "styles/util/dialog.less"
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
