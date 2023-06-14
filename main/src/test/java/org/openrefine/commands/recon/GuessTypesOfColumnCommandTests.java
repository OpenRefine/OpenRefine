
package org.openrefine.commands.recon;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;

import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Project;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class GuessTypesOfColumnCommandTests extends CommandTestBase {

    Project project = null;

    @BeforeMethod
    public void setUpCommand() {
        command = new GuessTypesOfColumnCommand();
        ((GuessTypesOfColumnCommand) command).setSampleSize(2);
        project = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "France", "b" },
                        { "Japan", "d" },
                        { "Paraguay", "x" }
                });

    }

    @Test
    public void testCSRFProtection() throws Exception {
        command.doPost(request, response);
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}");
    }

    @Test
    public void testGuessTypes() throws Exception {
        when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
        when(request.getParameter("columnName")).thenReturn("foo");
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        String expectedQuery = "queries=%7B%22q1%22%3A%7B%22query%22%3A%22Japan%22%2C%22limit%22" +
                "%3A3%7D%2C%22q0%22%3A%7B%22query%22%3A%22France%22%2C%22limit%22%3A3%7D%7D";

        String serviceResponse = "{\n" +
                "  \"q0\": {\n" +
                "    \"result\": [\n" +
                "      {\n" +
                "        \"id\": \"Q17\",\n" +
                "        \"name\": \"Japan\",\n" +
                "        \"type\": [\n" +
                "          {\n" +
                "            \"id\": \"Q3624078\",\n" +
                "            \"name\": \"sovereign state\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"Q112099\",\n" +
                "            \"name\": \"island nation\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"Q6256\",\n" +
                "            \"name\": \"country\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"q1\": {\n" +
                "    \"result\": [\n" +
                "      {\n" +
                "        \"id\": \"Q142\",\n" +
                "        \"name\": \"France\",\n" +
                "        \"type\": [\n" +
                "          {\n" +
                "            \"id\": \"Q3624078\",\n" +
                "            \"name\": \"sovereign state\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"Q20181813\",\n" +
                "            \"name\": \"colonial power\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        String guessedTypes = "{\n" +
                "       \"code\" : \"ok\",\n" +
                "       \"types\" : [ {\n" +
                "         \"count\" : 2,\n" +
                "         \"id\" : \"Q3624078\",\n" +
                "         \"name\" : \"sovereign state\",\n" +
                "         \"score\" : 2\n" +
                "       }, {\n" +
                "         \"count\" : 1,\n" +
                "         \"id\" : \"Q112099\",\n" +
                "         \"name\" : \"island nation\",\n" +
                "         \"score\" : 0.6666666666666666\n" +
                "       }, {\n" +
                "         \"count\" : 1,\n" +
                "         \"id\" : \"Q20181813\",\n" +
                "         \"name\" : \"colonial power\",\n" +
                "         \"score\" : 0.5\n" +
                "       }, {\n" +
                "         \"count\" : 1,\n" +
                "         \"id\" : \"Q6256\",\n" +
                "         \"name\" : \"country\",\n" +
                "         \"score\" : 0.3333333333333333\n" +
                "       } ]\n" +
                "     }";

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/api");
            server.enqueue(new MockResponse().setBody(serviceResponse));

            when(request.getParameter("service")).thenReturn(url.toString());

            command.doPost(request, response);

            verify(response).setStatus(200);
            TestUtils.assertEqualsAsJson(guessedTypes, writer.toString());

            RecordedRequest request = server.takeRequest();
            Assert.assertEquals(request.getBody().readUtf8(), expectedQuery);
        }
    }
}
