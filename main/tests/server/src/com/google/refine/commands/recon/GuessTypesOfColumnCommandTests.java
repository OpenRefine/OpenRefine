
package com.google.refine.commands.recon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.TestUtils;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class GuessTypesOfColumnCommandTests extends RefineTest {

    HttpServletRequest request = null;
    HttpServletResponse response = null;
    GuessTypesOfColumnCommand command = null;
    StringWriter writer = null;
    Project project = null;

    @BeforeMethod
    public void setUpCommand() {
        command = new GuessTypesOfColumnCommand();
        command.setSampleSize(2);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
        project = createCSVProject(
                "foo,bar\n"
                        + "France,b\n"
                        + "Japan,d\n"
                        + "Paraguay,x");

    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        TestUtils.assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }

    @Test
    public void testGuessTypes() throws IOException, ServletException, InterruptedException {
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
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

            TestUtils.assertEqualAsJson(guessedTypes, writer.toString());

            RecordedRequest request = server.takeRequest();
            Assert.assertEquals(request.getBody().readUtf8(), expectedQuery);
        }
    }
}
