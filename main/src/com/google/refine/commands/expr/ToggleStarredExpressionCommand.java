
package com.google.refine.commands.expr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

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
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");

                JSONWriter writer = new JSONWriter(response.getWriter());
                writer.object();
                writer.key("expressions");
                writer.array();
                for (String s : starredExpressions) {
                    writer.object();
                    writer.key("code");
                    writer.value(s);
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();
            } catch (Exception e) {
                respondException(response, e);
            }
        }
    }
}
