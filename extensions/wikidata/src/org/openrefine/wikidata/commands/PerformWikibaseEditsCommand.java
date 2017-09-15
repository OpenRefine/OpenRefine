package org.openrefine.wikidata.commands;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.openrefine.wikidata.operations.PerformWikibaseEditsOperation;
import org.openrefine.wikidata.operations.PerformWikibaseEditsOperation.DuplicateDetectionStrategy;
import org.openrefine.wikidata.operations.PerformWikibaseEditsOperation.OnDuplicateAction;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class PerformWikibaseEditsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project, HttpServletRequest request, JSONObject engineConfig)
            throws Exception {
        String strategy = request.getParameter("strategy");
        String action = request.getParameter("action");
        return new PerformWikibaseEditsOperation(engineConfig,
                DuplicateDetectionStrategy.valueOf(strategy),
                OnDuplicateAction.valueOf(action));
    }

}
