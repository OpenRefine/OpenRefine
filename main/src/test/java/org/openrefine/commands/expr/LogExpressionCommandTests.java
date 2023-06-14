
package org.openrefine.commands.expr;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

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
        prefStore = ProjectManager.singleton.getPreferenceStore();
    }

    @Override
    protected void cleanupProjectsAndJobs() {
        // do nothing, since ProjectManager.singleton was overriden by us
    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testNullExpressions() throws Exception {
        prefStore.put("scripting.expressions", null);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter("expression")).thenReturn("grel:value+'a'");

        command.doPost(request, response);

        verify(response).setStatus(200);
        TopList topList = (TopList) prefStore.get("scripting.expressions");
        Assert.assertEquals(topList.getList(), Collections.singletonList("grel:value+'a'"));
    }
}
