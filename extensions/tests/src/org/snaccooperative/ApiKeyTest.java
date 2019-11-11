package org.openrefine.wikidata.commands;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import javax.servlet.ServletException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ApiKeyTest extends RefineTest {
    @Test
    public void testNoCredentials() throws ServletException, IOException {
        command.doPost(request, response);
        assertEquals("null", writer.toString());
    }
}