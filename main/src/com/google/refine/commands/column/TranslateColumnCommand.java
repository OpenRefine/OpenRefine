
package com.google.refine.commands.column;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.column.TranslateColumnOperation;
import com.google.refine.process.Process;
import com.google.refine.translate.TranslationLanguage;

public class TranslateColumnCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            AbstractOperation operation = createOperation(request);
            operation.validate();
            Process process = operation.createProcess(project, new Properties());
            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    AbstractOperation createOperation(HttpServletRequest request) {
        return new TranslateColumnOperation(
                request.getParameter("columnName"),
                TranslationLanguage.fromName(request.getParameter("language")),
                request.getParameter("endpoint"),
                request.getParameter("model"),
                getRequiredIntegerParameter(request, "batchSize"),
                getRequiredIntegerParameter(request, "timeoutMillis"),
                getRequiredIntegerParameter(request, "maxRetries"),
                request.getParameter("apiKey"));
    }

    private int getRequiredIntegerParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + name);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer", e);
        }
    }
}
