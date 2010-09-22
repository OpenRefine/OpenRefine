var html = "text/html";
var encoding = "UTF-8";
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;
var bundle = true;

var templatedFiles = {
    // Requests with last path segments mentioned here 
    // will get served from .vt files with the same names
    "index" : true,
    "project" : true,
    "preferences" : true
};

function registerCommands() {
    var GS = Packages.com.google.refine.GridworksServlet;
    
    GS.registerCommand(module, "create-project-from-upload", new Packages.com.google.refine.commands.project.CreateProjectCommand());
    GS.registerCommand(module, "import-project", new Packages.com.google.refine.commands.project.ImportProjectCommand());
    GS.registerCommand(module, "export-project", new Packages.com.google.refine.commands.project.ExportProjectCommand());
    GS.registerCommand(module, "export-rows", new Packages.com.google.refine.commands.project.ExportRowsCommand());

    GS.registerCommand(module, "get-project-metadata", new Packages.com.google.refine.commands.project.GetProjectMetadataCommand());
    GS.registerCommand(module, "get-all-project-metadata", new Packages.com.google.refine.commands.workspace.GetAllProjectMetadataCommand());

    GS.registerCommand(module, "delete-project", new Packages.com.google.refine.commands.project.DeleteProjectCommand());
    GS.registerCommand(module, "rename-project", new Packages.com.google.refine.commands.project.RenameProjectCommand());

    GS.registerCommand(module, "get-models", new Packages.com.google.refine.commands.project.GetModelsCommand());
    GS.registerCommand(module, "get-rows", new Packages.com.google.refine.commands.row.GetRowsCommand());
    GS.registerCommand(module, "get-processes", new Packages.com.google.refine.commands.history.GetProcessesCommand());
    GS.registerCommand(module, "get-history", new Packages.com.google.refine.commands.history.GetHistoryCommand());
    GS.registerCommand(module, "get-operations", new Packages.com.google.refine.commands.history.GetOperationsCommand());
    GS.registerCommand(module, "get-columns-info", new Packages.com.google.refine.commands.column.GetColumnsInfoCommand());
    GS.registerCommand(module, "get-scatterplot", new Packages.com.google.refine.commands.browsing.GetScatterplotCommand());

    GS.registerCommand(module, "undo-redo", new Packages.com.google.refine.commands.history.UndoRedoCommand());
    GS.registerCommand(module, "apply-operations", new Packages.com.google.refine.commands.history.ApplyOperationsCommand());
    GS.registerCommand(module, "cancel-processes", new Packages.com.google.refine.commands.history.CancelProcessesCommand());

    GS.registerCommand(module, "compute-facets", new Packages.com.google.refine.commands.browsing.ComputeFacetsCommand());
    GS.registerCommand(module, "compute-clusters", new Packages.com.google.refine.commands.browsing.ComputeClustersCommand());

    GS.registerCommand(module, "edit-one-cell", new Packages.com.google.refine.commands.cell.EditOneCellCommand());
    GS.registerCommand(module, "text-transform", new Packages.com.google.refine.commands.cell.TextTransformCommand());
    GS.registerCommand(module, "mass-edit", new Packages.com.google.refine.commands.cell.MassEditCommand());
    GS.registerCommand(module, "join-multi-value-cells", new Packages.com.google.refine.commands.cell.JoinMultiValueCellsCommand());
    GS.registerCommand(module, "split-multi-value-cells", new Packages.com.google.refine.commands.cell.SplitMultiValueCellsCommand());
    GS.registerCommand(module, "fill-down", new Packages.com.google.refine.commands.cell.FillDownCommand());
    GS.registerCommand(module, "blank-down", new Packages.com.google.refine.commands.cell.BlankDownCommand());
    GS.registerCommand(module, "transpose-columns-into-rows", new Packages.com.google.refine.commands.cell.TransposeColumnsIntoRowsCommand());
    GS.registerCommand(module, "transpose-rows-into-columns", new Packages.com.google.refine.commands.cell.TransposeRowsIntoColumnsCommand());

    GS.registerCommand(module, "add-column", new Packages.com.google.refine.commands.column.AddColumnCommand());
    GS.registerCommand(module, "remove-column", new Packages.com.google.refine.commands.column.RemoveColumnCommand());
    GS.registerCommand(module, "rename-column", new Packages.com.google.refine.commands.column.RenameColumnCommand());
    GS.registerCommand(module, "move-column", new Packages.com.google.refine.commands.column.MoveColumnCommand());
    GS.registerCommand(module, "split-column", new Packages.com.google.refine.commands.column.SplitColumnCommand());
    GS.registerCommand(module, "extend-data", new Packages.com.google.refine.commands.column.ExtendDataCommand());
    GS.registerCommand(module, "add-column-by-fetching-urls", new Packages.com.google.refine.commands.column.AddColumnByFetchingURLsCommand());
    GS.registerCommand(module, "reorder-columns", new Packages.com.google.refine.commands.column.ReorderColumnsCommand());

    GS.registerCommand(module, "denormalize", new Packages.com.google.refine.commands.row.DenormalizeCommand());

    GS.registerCommand(module, "reconcile", new Packages.com.google.refine.commands.recon.ReconcileCommand());
    GS.registerCommand(module, "recon-match-best-candidates", new Packages.com.google.refine.commands.recon.ReconMatchBestCandidatesCommand());
    GS.registerCommand(module, "recon-mark-new-topics", new Packages.com.google.refine.commands.recon.ReconMarkNewTopicsCommand());
    GS.registerCommand(module, "recon-discard-judgments", new Packages.com.google.refine.commands.recon.ReconDiscardJudgmentsCommand());
    GS.registerCommand(module, "recon-match-specific-topic-to-cells", new Packages.com.google.refine.commands.recon.ReconMatchSpecificTopicCommand());
    GS.registerCommand(module, "recon-judge-one-cell", new Packages.com.google.refine.commands.recon.ReconJudgeOneCellCommand());
    GS.registerCommand(module, "recon-judge-similar-cells", new Packages.com.google.refine.commands.recon.ReconJudgeSimilarCellsCommand());

    GS.registerCommand(module, "annotate-one-row", new Packages.com.google.refine.commands.row.AnnotateOneRowCommand());
    GS.registerCommand(module, "annotate-rows", new Packages.com.google.refine.commands.row.AnnotateRowsCommand());
    GS.registerCommand(module, "remove-rows", new Packages.com.google.refine.commands.row.RemoveRowsCommand());
    GS.registerCommand(module, "reorder-rows", new Packages.com.google.refine.commands.row.ReorderRowsCommand());

    GS.registerCommand(module, "save-protograph", new Packages.com.google.refine.commands.freebase.SaveProtographCommand());

    GS.registerCommand(module, "get-expression-language-info", new Packages.com.google.refine.commands.expr.GetExpressionLanguageInfoCommand());
    GS.registerCommand(module, "get-expression-history", new Packages.com.google.refine.commands.expr.GetExpressionHistoryCommand());
    GS.registerCommand(module, "log-expression", new Packages.com.google.refine.commands.expr.LogExpressionCommand());

    GS.registerCommand(module, "preview-expression", new Packages.com.google.refine.commands.expr.PreviewExpressionCommand());
    GS.registerCommand(module, "preview-extend-data", new Packages.com.google.refine.commands.column.PreviewExtendDataCommand());
    GS.registerCommand(module, "preview-protograph", new Packages.com.google.refine.commands.freebase.PreviewProtographCommand());

    GS.registerCommand(module, "guess-types-of-column", new Packages.com.google.refine.commands.freebase.GuessTypesOfColumnCommand());

    GS.registerCommand(module, "check-authorization", new Packages.com.google.refine.commands.auth.CheckAuthorizationCommand());
    GS.registerCommand(module, "authorize", new Packages.com.google.refine.commands.auth.AuthorizeCommand());
    GS.registerCommand(module, "deauthorize", new Packages.com.google.refine.commands.auth.DeAuthorizeCommand());
    GS.registerCommand(module, "user-badges", new Packages.com.google.refine.commands.auth.GetUserBadgesCommand());

    GS.registerCommand(module, "upload-data", new Packages.com.google.refine.commands.freebase.UploadDataCommand());
    GS.registerCommand(module, "import-qa-data", new Packages.com.google.refine.commands.freebase.ImportQADataCommand());
    GS.registerCommand(module, "mqlread", new Packages.com.google.refine.commands.freebase.MQLReadCommand());
    GS.registerCommand(module, "mqlwrite", new Packages.com.google.refine.commands.freebase.MQLWriteCommand());

    GS.registerCommand(module, "get-preference", new Packages.com.google.refine.commands.GetPreferenceCommand());
    GS.registerCommand(module, "get-all-preferences", new Packages.com.google.refine.commands.GetAllPreferencesCommand());
    GS.registerCommand(module, "set-preference", new Packages.com.google.refine.commands.SetPreferenceCommand());
    GS.registerCommand(module, "open-workspace-dir", new Packages.com.google.refine.commands.OpenWorkspaceDirCommand());
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
    
    OR.registerOperation(module, "column-addition", Packages.com.google.refine.operations.column.ColumnAdditionOperation);
    OR.registerOperation(module, "column-removal", Packages.com.google.refine.operations.column.ColumnRemovalOperation);
    OR.registerOperation(module, "column-rename", Packages.com.google.refine.operations.column.ColumnRenameOperation);
    OR.registerOperation(module, "column-move", Packages.com.google.refine.operations.column.ColumnMoveOperation);
    OR.registerOperation(module, "column-split", Packages.com.google.refine.operations.column.ColumnSplitOperation);
    OR.registerOperation(module, "extend-data", Packages.com.google.refine.operations.column.ExtendDataOperation);
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
    OR.registerOperation(module, "import-qa-data", Packages.com.google.refine.operations.recon.ImportQADataOperation);
    
    OR.registerOperation(module, "save-protograph", Packages.com.google.refine.operations.SaveProtographOperation);
}

/*
 *  This optional function is invoked from the module's init() Java function.
 */
function init() {
    // Packages.java.lang.System.err.println("Initializing by script " + module);
    
    registerCommands();
    registerOperations();
    
    ClientSideResourceManager.addPaths(
        "index/scripts",
        module,
        [
            "externals/jquery-1.4.2.min.js",
            "externals/jquery-ui/jquery-ui-1.8.custom.min.js",
            "externals/date.js",
            "scripts/util/string.js",
            "scripts/version.js",
            "scripts/index.js"
        ]
    );
    
    ClientSideResourceManager.addPaths(
        "index/styles",
        module,
        [
            "externals/jquery-ui/css/ui-lightness/jquery-ui-1.8.custom.css",
            "styles/common.css",
            "styles/freebase.css",
            "styles/index.css",
            "styles/jquery-ui-overrides.css"
        ]
    );
    
    ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "externals/jquery-1.4.2.min.js",
            "externals/jquery.cookie.js",
            "externals/suggest/suggest-1.2.min.js",
            "externals/jquery-ui/jquery-ui-1.8.custom.min.js",
            "externals/imgareaselect/jquery.imgareaselect.js",
            "externals/date.js",
            
            "scripts/project.js",
            
            "scripts/util/misc.js",
            "scripts/util/url.js",
            "scripts/util/string.js",
            "scripts/util/ajax.js",
            "scripts/util/menu.js",
            "scripts/util/dialog.js",
            "scripts/util/dom.js",
            "scripts/util/sign.js",
            "scripts/util/freebase.js",
            "scripts/util/custom-suggest.js",

            "scripts/widgets/history-widget.js",
            "scripts/widgets/process-widget.js",
            "scripts/widgets/histogram-widget.js",
            "scripts/widgets/slider-widget.js",

            "scripts/project/menu-bar.js",
            "scripts/project/browsing-engine.js",
            "scripts/project/scripting.js",

            "scripts/facets/list-facet.js",
            "scripts/facets/range-facet.js",
            "scripts/facets/timerange-facet.js",
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
            "scripts/reconciliation/freebase-query-panel.js",
            "scripts/reconciliation/standard-service-panel.js",
            
            "scripts/dialogs/expression-preview-dialog.js",
            "scripts/dialogs/freebase-loading-dialog.js",
            "scripts/dialogs/clustering-dialog.js",
            "scripts/dialogs/scatterplot-dialog.js",
            "scripts/dialogs/extend-data-preview-dialog.js",
            "scripts/dialogs/templating-exporter-dialog.js",
            "scripts/dialogs/column-reordering-dialog.js",

            "scripts/protograph/schema-alignment.js",
            "scripts/protograph/schema-alignment-ui-node.js",
            "scripts/protograph/schema-alignment-ui-link.js"
        ]
    );
    
    ClientSideResourceManager.addPaths(
        "project/styles",
        module,
        [
            "externals/suggest/css/suggest-1.2.min.css",
            "externals/jquery-ui/css/ui-lightness/jquery-ui-1.8.custom.css",
            "externals/imgareaselect/css/imgareaselect-default.css",
    
            "styles/common.css",
            "styles/jquery-ui-overrides.css",
    
            "styles/util/menu.css",
            "styles/util/dialog.css",
            "styles/util/custom-suggest.css",

            "styles/project.css",
            "styles/project/browsing.css",
            "styles/project/process.css",
            "styles/project/menu-bar.css",
            
            "styles/widgets/history.css",
            "styles/widgets/histogram-widget.css",
            "styles/widgets/slider-widget.css",
            
            "styles/views/data-table-view.css",
            
            "styles/dialogs/expression-preview-dialog.css",
            "styles/dialogs/clustering-dialog.css",
            "styles/dialogs/scatterplot-dialog.css",
            "styles/dialogs/freebase-loading-dialog.css",
            "styles/dialogs/extend-data-preview-dialog.css",
            "styles/dialogs/column-reordering-dialog.css",
            
            "styles/reconciliation/recon-dialog.css",
            "styles/reconciliation/standard-service-panel.css",
            
            "styles/protograph/schema-alignment-dialog.css"
        ]
    );
    
    ClientSideResourceManager.addPaths(
        "preferences/scripts",
        module,
        [
            "externals/jquery-1.4.2.min.js",
            "externals/jquery.cookie.js",
            "externals/suggest/suggest-1.2.min.js",
            "externals/jquery-ui/jquery-ui-1.8.custom.min.js",
            "externals/imgareaselect/jquery.imgareaselect.js",
            "externals/date.js",
            "scripts/preferences.js"
        ]
    );
    ClientSideResourceManager.addPaths(
        "preferences/styles",
        module,
        [
            "externals/suggest/css/suggest-1.2.min.css",
            "externals/jquery-ui/css/ui-lightness/jquery-ui-1.8.custom.css",
            
            "styles/common.css",
            "styles/jquery-ui-overrides.css",
            "styles/preferences.css"
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
            path = "/index";
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
                for each (var qualifiedPath in paths) {
                    var input = null;
                    try {
                        var url = qualifiedPath.module.getResource(qualifiedPath.path);
                        var urlConnection = url.openConnection();
                        
                        input = new Packages.java.io.BufferedReader(
                            new Packages.java.io.InputStreamReader(urlConnection.getInputStream()));
                            
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
                context.projectID = request.getParameter("project");
                
                var styles = ClientSideResourceManager.getPaths(lastSegment + "/styles");
                var styleInjection = [];
                for each (var qualifiedPath in styles) {
                    styleInjection.push(
                        '<link type="text/css" rel="stylesheet" href="' + qualifiedPath.fullPath + '" />');
                }
                context.styleInjection = styleInjection.join("\n");
                
                if (bundle) {
                    context.scriptInjection = '<script type="text/javascript" src="' + path + '-bundle.js"></script>';
                } else {
                    var scripts = ClientSideResourceManager.getPaths(lastSegment + "/scripts");
                    var scriptInjection = [];
                    for each (var qualifiedPath in scripts) {
                        scriptInjection.push(
                            '<script type="text/javascript" src="' + qualifiedPath.fullPath + '"></script>');
                    }
                    context.scriptInjection = scriptInjection.join("\n");
                }
            
                send(request, response, path + ".vt", context);
            }
        }
    }
}

function send(request, response, template, context) {
    butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
