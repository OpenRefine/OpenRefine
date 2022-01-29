
package com.google.refine.commands.expr;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
