package com.metaweb.gridworks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.commands.Command;

public class GridworksServlet extends HttpServlet {
    
    private static final long serialVersionUID = 2386057901503517403L;
    
    static final private Map<String, Command> commands = new HashMap<String, Command>();
    
    // timer for periodically saving projects
    static private Timer _timer;

    final static Logger logger = LoggerFactory.getLogger("servlet");

    // TODO: This belongs in an external config file somewhere
    private static final String[][] commandNames = {
        {"create-project-from-upload", "com.metaweb.gridworks.commands.project.CreateProjectCommand"},
        {"import-project", "com.metaweb.gridworks.commands.project.ImportProjectCommand"},
        {"export-project", "com.metaweb.gridworks.commands.project.ExportProjectCommand"},
        {"export-rows", "com.metaweb.gridworks.commands.project.ExportRowsCommand"},
        
        {"get-project-metadata", "com.metaweb.gridworks.commands.project.GetProjectMetadataCommand"},
        {"get-all-project-metadata", "com.metaweb.gridworks.commands.workspace.GetAllProjectMetadataCommand"},

        {"delete-project", "com.metaweb.gridworks.commands.project.DeleteProjectCommand"},
        {"rename-project", "com.metaweb.gridworks.commands.project.RenameProjectCommand"},
        
        {"get-models", "com.metaweb.gridworks.commands.project.GetModelsCommand"},
        {"get-rows", "com.metaweb.gridworks.commands.row.GetRowsCommand"},
        {"get-processes", "com.metaweb.gridworks.commands.history.GetProcessesCommand"},
        {"get-history", "com.metaweb.gridworks.commands.history.GetHistoryCommand"},
        {"get-operations", "com.metaweb.gridworks.commands.history.GetOperationsCommand"},
        {"get-columns-info", "com.metaweb.gridworks.commands.column.GetColumnsInfoCommand"},
        {"get-scatterplot", "com.metaweb.gridworks.commands.browsing.GetScatterplotCommand"},
        
        {"undo-redo", "com.metaweb.gridworks.commands.history.UndoRedoCommand"},
        {"apply-operations", "com.metaweb.gridworks.commands.history.ApplyOperationsCommand"},
        {"cancel-processes", "com.metaweb.gridworks.commands.history.CancelProcessesCommand"},
        
        {"compute-facets", "com.metaweb.gridworks.commands.browsing.ComputeFacetsCommand"},
        {"compute-clusters", "com.metaweb.gridworks.commands.browsing.ComputeClustersCommand"},
        
        {"edit-one-cell", "com.metaweb.gridworks.commands.cell.EditOneCellCommand"},
        {"text-transform", "com.metaweb.gridworks.commands.cell.TextTransformCommand"},
        {"mass-edit", "com.metaweb.gridworks.commands.cell.MassEditCommand"},
        {"join-multi-value-cells", "com.metaweb.gridworks.commands.cell.JoinMultiValueCellsCommand"},
        {"split-multi-value-cells", "com.metaweb.gridworks.commands.cell.SplitMultiValueCellsCommand"},
        
        {"add-column", "com.metaweb.gridworks.commands.column.AddColumnCommand"},
        {"remove-column", "com.metaweb.gridworks.commands.column.RemoveColumnCommand"},
        {"rename-column", "com.metaweb.gridworks.commands.column.RenameColumnCommand"},
        {"split-column", "com.metaweb.gridworks.commands.column.SplitColumnCommand"},
        {"extend-data", "com.metaweb.gridworks.commands.column.ExtendDataCommand"},
        
        {"denormalize", "com.metaweb.gridworks.commands.row.DenormalizeCommand"},
        
        {"reconcile", "com.metaweb.gridworks.commands.recon.ReconcileCommand"},
        {"recon-match-best-candidates", "com.metaweb.gridworks.commands.recon.ReconMatchBestCandidatesCommand"},
        {"recon-mark-new-topics", "com.metaweb.gridworks.commands.recon.ReconMarkNewTopicsCommand"},
        {"recon-discard-judgments", "com.metaweb.gridworks.commands.recon.ReconDiscardJudgmentsCommand"},
        {"recon-match-specific-topic-to-cells", "com.metaweb.gridworks.commands.recon.ReconMatchSpecificTopicCommand"},
        {"recon-judge-one-cell", "com.metaweb.gridworks.commands.recon.ReconJudgeOneCellCommand"},
        {"recon-judge-similar-cells", "com.metaweb.gridworks.commands.recon.ReconJudgeSimilarCellsCommand"},
        
        {"annotate-one-row", "com.metaweb.gridworks.commands.row.AnnotateOneRowCommand"},
        {"annotate-rows", "com.metaweb.gridworks.commands.row.AnnotateRowsCommand"},
        {"remove-rows", "com.metaweb.gridworks.commands.row.RemoveRowsCommand"},
        {"reorder-rows", "com.metaweb.gridworks.commands.row.ReorderRowsCommand"},
        
        {"save-protograph", "com.metaweb.gridworks.commands.freebase.SaveProtographCommand"},
        
        {"get-expression-language-info", "com.metaweb.gridworks.commands.expr.GetExpressionLanguageInfoCommand"},
        {"get-expression-history", "com.metaweb.gridworks.commands.expr.GetExpressionHistoryCommand"},
        {"log-expression", "com.metaweb.gridworks.commands.expr.LogExpressionCommand"},
        
        {"preview-expression", "com.metaweb.gridworks.commands.expr.PreviewExpressionCommand"},
        {"preview-extend-data", "com.metaweb.gridworks.commands.column.PreviewExtendDataCommand"},
        {"preview-protograph", "com.metaweb.gridworks.commands.freebase.PreviewProtographCommand"},
        
        {"guess-types-of-column", "com.metaweb.gridworks.commands.freebase.GuessTypesOfColumnCommand"},
        
        {"check-authorization", "com.metaweb.gridworks.commands.auth.CheckAuthorizationCommand"},
        {"authorize", "com.metaweb.gridworks.commands.auth.AuthorizeCommand"},
        {"deauthorize", "com.metaweb.gridworks.commands.auth.DeAuthorizeCommand"},
        {"user-badges", "com.metaweb.gridworks.commands.auth.GetUserBadgesCommand"},

        {"upload-data", "com.metaweb.gridworks.commands.freebase.UploadDataCommand"},
        {"mqlread", "com.metaweb.gridworks.commands.freebase.MQLReadCommand"},
        {"mqlwrite", "com.metaweb.gridworks.commands.freebase.MQLWriteCommand"},
    };
    
    static {
        registerCommands(commandNames);
    }
    
    final static protected long s_autoSavePeriod = 1000 * 60 * 5; // 5 minutes
    static protected class AutoSaveTimerTask extends TimerTask {
        public void run() {
            try {
                ProjectManager.singleton.save(false); // quick, potentially incomplete save
            } finally {
                _timer.schedule(new AutoSaveTimerTask(), s_autoSavePeriod);
                // we don't use scheduleAtFixedRate because that might result in 
                // bunched up events when the computer is put in sleep mode
            }
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.trace("> initialize");
        
        ProjectManager.initialize();
                
        if (_timer == null) {
            _timer = new Timer("autosave");
            _timer.schedule(new AutoSaveTimerTask(), s_autoSavePeriod);
        }
        
        logger.trace("< initialize");
    }
    
    @Override
    public void destroy() {
        logger.trace("> destroy");

        // cancel automatic periodic saving and force a complete save. 
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
        if (ProjectManager.singleton != null) {
            ProjectManager.singleton.save(true); // complete save
            ProjectManager.singleton = null;
        }
        
        super.destroy();

        logger.trace("< destroy");
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String commandName = getCommandName(request);
        Command command = commands.get(commandName);
        if (command != null) {
            logger.trace("> GET {}", commandName);
            command.doGet(request, response);
            logger.trace("< GET {}", commandName);
        } else {
            response.sendError(404);
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String commandName = getCommandName(request);
        Command command = commands.get(commandName);
        if (command != null) {
            logger.trace("> POST {}", commandName);
            command.doPost(request, response);
            logger.trace("< POST {}", commandName);
        } else {
            response.sendError(404);
        }
    }
        
    protected String getCommandName(HttpServletRequest request) {
        // Remove extraneous path segments that might be there for other purposes,
        // e.g., for /export-rows/filename.ext, export-rows is the command while
        // filename.ext is only for the browser to prompt a convenient filename. 
        String commandName = request.getPathInfo().substring(1);
        int slash = commandName.indexOf('/');
        return slash > 0 ? commandName.substring(0, slash) : commandName;
    }
    
    /**
     * Register an array of commands
     * 
     * @param commands
     *            An array of arrays containing pairs of strings with the
     *            command name in the first element of the tuple and the fully
     *            qualified class name of the class implementing the command in
     *            the second.
     * @return false if any commands failed to load
     */
    static public boolean registerCommands(String[][] commands) {
        boolean status = true;
        for (String[] command : commandNames) {
            String commandName = command[0];
            String className = command[1];
            logger.debug("Loading command " + commandName + " class: " + className);
            Command cmd;
            try {
                // TODO: May need to use the servlet container's class loader here
                cmd = (Command) Class.forName(className).newInstance();
            } catch (InstantiationException e) {
                logger.error("Failed to load command class " + className, e);
                status = false;
                continue;
            } catch (IllegalAccessException e) {
                logger.error("Failed to load command class " + className, e);
                status = false;
                continue;
            } catch (ClassNotFoundException e) {
                logger.error("Failed to load command class " + className, e);
                status = false;
                continue;
            }
            status |= registerCommand(commandName, cmd);
        }
        return status;
    }
    
    /**
     * Register a single command.
     * 
     * @param name
     *            command verb for command
     * @param commandObject
     *            object implementing the command
     * @return true if command was loaded and registered successfully
     */
    static public boolean registerCommand(String name, 
            Command commandObject) {
        if (commands.containsKey(name)) {
            return false;
        }
        commands.put(name, commandObject);
        return true;
    }

    // Currently only for test purposes
    static protected boolean unregisterCommand(String verb) {
        return commands.remove(verb) != null;
    }
}

