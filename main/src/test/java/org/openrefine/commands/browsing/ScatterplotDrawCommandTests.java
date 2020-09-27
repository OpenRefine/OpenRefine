package org.openrefine.commands.browsing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.RefineTest;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.ScatterplotFacet.Dimension;
import org.openrefine.browsing.facets.ScatterplotFacet.Rotation;
import org.openrefine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import org.openrefine.commands.Command;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ScatterplotDrawCommandTests extends RefineTest {
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
	protected StringWriter writer = null;
	protected Command command = null;
	protected Project project = null;
	protected ServletOutputStream outputStream;
	
    @BeforeMethod
    public void setUp() {
    	FacetConfigResolver.registerFacetConfig("core", "scatterplot", ScatterplotFacetConfig.class);
    	MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = new GetScatterplotCommand();
        writer = new StringWriter();
        outputStream = mock(ServletOutputStream.class);
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
			when(response.getOutputStream()).thenReturn(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        project = createProject(new String[] {"a", "b"},
        		new Serializable[] {
        			0, 1,
        			2, 3	
        		});
    }
    
    public static String configJson = "{"
    		+ "\"name\":\"a (x) vs. b (y)\","
    		+ "\"cx\":\"a\","
    		+ "\"cy\":\"b\","
    		+ "\"l\":150,"
    		+ "\"ex\":\"value\","
    		+ "\"ey\":\"value\","
    		+ "\"dot\":0.8,"
    		+ "\"dim_x\":\"log\","
    		+ "\"dim_y\":\"lin\","
    		+ "\"type\":\"scatterplot\","
    		+ "\"from_x\":1,"
    		+ "\"to_x\":2,"
    		+ "\"from_y\":3,"
    		+ "\"to_y\":4,"
    		+ "\"color\":\"ff6a00\""
    		+ "}";
    
    
    public static String configJsonWithNone = "{"
    		+ "\"name\":\"b (x) vs. y (y)\","
    		+ "\"cx\":\"b\","
    		+ "\"cy\":\"y\","
    		+ "\"l\":150,"
    		+ "\"ex\":\"value\","
    		+ "\"ey\":\"value\","
    		+ "\"dot\":1.4,"
    		+ "\"dim_x\":\"lin\","
    		+ "\"dim_y\":\"lin\","
    		+ "\"r\":\"none\","
    		+ "\"type\":\"scatterplot\","
    		+ "\"from_x\":0,"
    		+ "\"to_x\":0,"
    		+ "\"from_y\":0,"
    		+ "\"to_y\":0,"
    		+ "\"color\":\"ff6a00\"}";
    
    @Test
    public void testParseConfig() throws JsonParseException, JsonMappingException, IOException {
    	ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
    	Assert.assertEquals("a", config.columnName_x);
    	Assert.assertEquals("b", config.columnName_y);
    	Assert.assertEquals(Dimension.LOG, config.dim_x);
    	Assert.assertEquals(Dimension.LIN, config.dim_y);
    }
    
    @Test
    public void testParseConfigWithNone() throws JsonParseException, JsonMappingException, IOException {
    	ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJsonWithNone, ScatterplotFacetConfig.class);
    	Assert.assertEquals(Rotation.NO_ROTATION, config.rotation);
    }
    
    @Test
    public void testDrawScatterplot() throws ServletException, IOException {
    	when(request.getParameter("project")).thenReturn(Long.toString(project.getId()));
    	when(request.getParameter("plotter")).thenReturn(configJson);
    	when(request.getParameter("engineConfig")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
    	
    	command.doGet(request, response);
    	// Not sure how to check the resulting image - at least this test ensures that no exception was thrown
    	Assert.assertEquals(writer.toString(), "");
    }
	
}
