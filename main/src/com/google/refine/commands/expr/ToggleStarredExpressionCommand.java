
package com.google.refine.commands.expr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.TopList;

public class ToggleStarredExpressionCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expression = request.getParameter("expression");

        TopList starredExpressions = ((TopList) ProjectManager.singleton.getPreferenceStore().get(
                "scripting.starred-expressions"));

        if (starredExpressions.getList().contains(expression)) {
            starredExpressions.remove(expression);
        } else {
            starredExpressions.add(expression);
        }

        if(request.getParameter("returnList") != null) {
            try {
                respondJSON(response, GetStarredExpressionsCommand.getExpressionsList());
            } catch (Exception e) {
                respondException(response, e);
            }
        }
    }
}
