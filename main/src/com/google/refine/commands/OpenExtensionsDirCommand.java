
package com.google.refine.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.io.FileProjectManager;

public class OpenExtensionsDirCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        String serverName = request.getServerName();

        if (!"127.0.0.1".equals(serverName) && !"localhost".equals(serverName)) {
            respond(response,
                    "{ \"code\" : \"error\", \"message\" : \"Extension directory can only be opened on the local machine where OpenRefine is run.\" }");
        } else if (ProjectManager.singleton instanceof FileProjectManager) {
            File dir = new File(((FileProjectManager) ProjectManager.singleton).getWorkspaceDir(), "extensions");

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(dir);
            } else /* if Mac */ {
                Runtime.getRuntime().exec(
                        "open .",
                        new String[] {},
                        dir);
            }

            respond(response, "{ \"code\" : \"ok\" }");
        } else {
            respond(response, "{ \"code\" : \"error\", \"message\" : \"Workspace is not stored on the file system.\" }");
        }
    }

}