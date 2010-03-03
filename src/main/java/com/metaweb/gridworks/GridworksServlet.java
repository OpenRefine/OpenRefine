package com.metaweb.gridworks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.commands.edit.AddColumnCommand;
import com.metaweb.gridworks.commands.edit.AnnotateOneRowCommand;
import com.metaweb.gridworks.commands.edit.AnnotateRowsCommand;
import com.metaweb.gridworks.commands.edit.ApplyOperationsCommand;
import com.metaweb.gridworks.commands.edit.CreateProjectCommand;
import com.metaweb.gridworks.commands.edit.DoTextTransformCommand;
import com.metaweb.gridworks.commands.edit.FacetBasedEditCommand;
import com.metaweb.gridworks.commands.edit.JoinMultiValueCellsCommand;
import com.metaweb.gridworks.commands.edit.RemoveColumnCommand;
import com.metaweb.gridworks.commands.edit.SaveProtographCommand;
import com.metaweb.gridworks.commands.edit.SplitMultiValueCellsCommand;
import com.metaweb.gridworks.commands.edit.UndoRedoCommand;
import com.metaweb.gridworks.commands.info.ComputeFacetsCommand;
import com.metaweb.gridworks.commands.info.ExportRowsCommand;
import com.metaweb.gridworks.commands.info.GetAllProjectMetadataCommand;
import com.metaweb.gridworks.commands.info.GetHistoryCommand;
import com.metaweb.gridworks.commands.info.GetModelsCommand;
import com.metaweb.gridworks.commands.info.GetOperationsCommand;
import com.metaweb.gridworks.commands.info.GetProcessesCommand;
import com.metaweb.gridworks.commands.info.GetProjectMetadataCommand;
import com.metaweb.gridworks.commands.info.GetRowsCommand;
import com.metaweb.gridworks.commands.recon.ReconDiscardJudgmentsCommand;
import com.metaweb.gridworks.commands.recon.ReconJudgeOneCellCommand;
import com.metaweb.gridworks.commands.recon.ReconJudgeSimilarCellsCommand;
import com.metaweb.gridworks.commands.recon.ReconMarkNewTopicsCommand;
import com.metaweb.gridworks.commands.recon.ReconMatchBestCandidatesCommand;
import com.metaweb.gridworks.commands.recon.ReconMatchSpecificTopicCommand;
import com.metaweb.gridworks.commands.recon.ReconcileCommand;
import com.metaweb.gridworks.commands.util.CancelProcessesCommand;
import com.metaweb.gridworks.commands.util.GetExpressionLanguageInfoCommand;
import com.metaweb.gridworks.commands.util.GuessTypesOfColumnCommand;
import com.metaweb.gridworks.commands.util.PreviewExpressionCommand;
import com.metaweb.gridworks.commands.util.PreviewProtographCommand;

public class GridworksServlet extends HttpServlet {
    
    private static final long serialVersionUID = 2386057901503517403L;
    
    static protected Map<String, Command> _commands = new HashMap<String, Command>();
    
    static {
        _commands.put("create-project-from-upload", new CreateProjectCommand());
        _commands.put("export-rows", new ExportRowsCommand());
        
        _commands.put("get-project-metadata", new GetProjectMetadataCommand());
        _commands.put("get-all-project-metadata", new GetAllProjectMetadataCommand());
        
        _commands.put("get-models", new GetModelsCommand());
        _commands.put("get-rows", new GetRowsCommand());
        _commands.put("get-processes", new GetProcessesCommand());
        _commands.put("get-history", new GetHistoryCommand());
        _commands.put("get-operations", new GetOperationsCommand());
        
        _commands.put("undo-redo", new UndoRedoCommand());
        _commands.put("apply-operations", new ApplyOperationsCommand());
        _commands.put("cancel-processes", new CancelProcessesCommand());
        
        _commands.put("compute-facets", new ComputeFacetsCommand());
        _commands.put("do-text-transform", new DoTextTransformCommand());
        _commands.put("facet-based-edit", new FacetBasedEditCommand());
        
        _commands.put("add-column", new AddColumnCommand());
        _commands.put("remove-column", new RemoveColumnCommand());
        _commands.put("join-multi-value-cells", new JoinMultiValueCellsCommand());
        _commands.put("split-multi-value-cells", new SplitMultiValueCellsCommand());
        
        _commands.put("reconcile", new ReconcileCommand());
        _commands.put("recon-match-best-candidates", new ReconMatchBestCandidatesCommand());
        _commands.put("recon-mark-new-topics", new ReconMarkNewTopicsCommand());
        _commands.put("recon-discard-judgments", new ReconDiscardJudgmentsCommand());
        _commands.put("recon-match-specific-topic-to-cells", new ReconMatchSpecificTopicCommand());
        _commands.put("recon-judge-one-cell", new ReconJudgeOneCellCommand());
        _commands.put("recon-judge-similar-cells", new ReconJudgeSimilarCellsCommand());
        
        _commands.put("annotate-one-row", new AnnotateOneRowCommand());
        _commands.put("annotate-rows", new AnnotateRowsCommand());
        
        _commands.put("save-protograph", new SaveProtographCommand());
        
        _commands.put("preview-expression", new PreviewExpressionCommand());
        _commands.put("get-expression-language-info", new GetExpressionLanguageInfoCommand());
        _commands.put("preview-protograph", new PreviewProtographCommand());
        _commands.put("guess-types-of-column", new GuessTypesOfColumnCommand());
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }
    
    @Override
    public void destroy() {
        if (ProjectManager.singleton != null) {
            ProjectManager.singleton.saveAllProjects();
            ProjectManager.singleton.save();
            ProjectManager.singleton = null;
        }
        
        super.destroy();
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProjectManager.initialize();
        
        String commandName = request.getPathInfo().substring(1);
        Command command = _commands.get(commandName);
        if (command != null) {
            command.doPost(request, response);
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProjectManager.initialize();
        
        String commandName = request.getPathInfo().substring(1);
        Command command = _commands.get(commandName);
        if (command != null) {
            command.doGet(request, response);
        }
    }
    
}
