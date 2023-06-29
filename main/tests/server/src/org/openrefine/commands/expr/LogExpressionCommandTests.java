
package org.openrefine.commands.expr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.preference.PreferenceStore;
import org.openrefine.preference.TopList;

public class LogExpressionCommandTests extends CommandTestBase {

    PreferenceStore prefStore;

    @BeforeMethod
    public void setUpCommand() {
        command = new LogExpressionCommand();
        ProjectManager.singleton = mock(ProjectManager.class);
        prefStore = new PreferenceStore();
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testNullExpressions() throws ServletException, IOException {
        prefStore.put("scripting.expressions", null);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("expression")).thenReturn("grel:value+'a'");

        command.doPost(request, response);

        TopList topList = (TopList) prefStore.get("scripting.expressions");
        Assert.assertEquals(topList.getList(), Collections.singletonList("grel:value+'a'"));
    }
}
