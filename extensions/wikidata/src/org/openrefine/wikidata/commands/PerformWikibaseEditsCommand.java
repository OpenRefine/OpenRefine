package org.openrefine.wikidata.commands;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.openrefine.wikidata.operations.PerformWikibaseEditsOperation;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class PerformWikibaseEditsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project, HttpServletRequest request, JSONObject engineConfig)
            throws Exception {
        String summary = request.getParameter("summary");
        return new PerformWikibaseEditsOperation(engineConfig,
                summary);
    }

}
