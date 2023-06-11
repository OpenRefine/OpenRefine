
package org.openrefine.commands.recon;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class PreviewExtendDataCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new PreviewExtendDataCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testPreviewExtendData() throws IOException, ServletException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/api");

            String endpoint = url.toString();
            Grid grid = createGrid(new String[] { "a", "b" },
                    new Serializable[][] {
                            { recon("foo", "Q123", endpoint), "d" },
                            { "e", "f" },
                            { recon("bar", "Q345", endpoint), "h" }
                    });

            String serviceResponse = "{"
                    + "   \"meta\": [{\"id\":\"P123\",\"name\":\"Property\",\"type\":{\"id\":\"Q5\",\"name\":\"human\"}}],"
                    + "   \"rows\": {"
                    + "       \"Q123\": {\"P123\": [{\"str\":\"hello\"}]},"
                    + "       \"Q345\": {\"P123\": [{\"str\":\"world\"}]}"
                    + "   }"
                    + "}";

            server.enqueue(new MockResponse().setBody(serviceResponse));

            StandardReconConfig reconConfig = new StandardReconConfig(endpoint, endpoint, endpoint, null, false, null, 0);
            ColumnModel columnModel = grid.getColumnModel().withReconConfig(0, reconConfig);
            grid = grid.withColumnModel(columnModel);
            Project project = createProject("test project", grid);

            when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
            when(request.getParameter("columnName")).thenReturn("a");
            when(request.getParameter("limit")).thenReturn("3");
            when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
            when(request.getParameter("extension")).thenReturn("{\"properties\":[{\"id\":\"P123\",\"name\":\"Property\"}]}");
            when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

            command.doPost(request, response);

            String expectedResponse = "{\n"
                    + "       \"code\" : \"ok\",\n"
                    + "       \"columns\" : [ {\n"
                    + "         \"type\" : {\"id\":\"Q5\",\"name\":\"human\"},\n"
                    + "         \"id\" : \"P123\",\n"
                    + "         \"name\" : \"Property\"\n"
                    + "       } ],\n"
                    + "       \"rows\" : [ [ \"foo 1\", \"hello\" ], [ \"<not reconciled>\" ], [ \"bar 1\", \"world\" ] ]\n"
                    + "     }";

            TestUtils.assertEqualsAsJson(expectedResponse, writer.toString());
        }
    }

    protected Cell recon(String name, String id, String endpoint) {
        List<ReconCandidate> candidates = Arrays.asList(
                new ReconCandidate(id, name + " 1", null, 98.0),
                new ReconCandidate(id + "2", name + " 2", null, 76.0));
        ReconCandidate match = candidates.get(0);
        Recon recon = new Recon(
                1234L,
                3478L,
                Judgment.Matched,
                match,
                new Object[3],
                candidates,
                endpoint,
                endpoint,
                endpoint,
                "batch",
                -1);
        return new Cell(name, recon);
    }
}
