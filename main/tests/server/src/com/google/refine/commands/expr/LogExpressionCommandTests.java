
package com.google.refine.commands.expr;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.Project;
import com.google.refine.model.ProjectStub;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;

public class LogExpressionCommandTests extends CommandTestBase {

    private static long PROJECT_ID = 1234;
    ProjectManager projectManager = null;
    Project project = null;

    PreferenceStore prefStore;

    @BeforeMethod
    public void setUpCommand() {
        command = new LogExpressionCommand();

        ProjectMetadata metadata = new ProjectMetadata();
        projectManager = new ProjectManagerStub();
        ProjectManager.singleton = projectManager;
        project = new ProjectStub(PROJECT_ID);
        ProjectManager.singleton.registerProject(project, metadata);
        prefStore = ProjectManager.singleton.getPreferenceStore();

        when(request.getParameter("project")).thenReturn(Long.toString(PROJECT_ID));

    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testNullExpressions() throws ServletException, IOException {
        prefStore.put("scripting.expressions", null);
        when(request.getParameter("project")).thenReturn(Long.toString(PROJECT_ID));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("expression")).thenReturn("grel:value+'a'");

        command.doPost(request, response);

        TopList globalExpressions = (TopList) prefStore.get("scripting.expressions");
        Assert.assertEquals(globalExpressions.getList(), Collections.singletonList("grel:value+'a'"));

        TopList localExpressions = (TopList) project.getMetadata().getPreferenceStore().get("scripting.expressions");
        Assert.assertEquals(localExpressions, Collections.singletonList("grel:value+'a'"));
    }

    // TODO: Add tests for starred, local, & global expressions
}
