
package com.google.refine.commands.browsing;

import java.io.IOException;

import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.browsing.facets.ScatterplotFacet;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.util.ParsingUtilities;

public class ScatterplotDrawCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new GetScatterplotCommand();
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

    public static String configJsonWithCW = "{"
            + "\"name\":\"b (x) vs. y (y)\","
            + "\"cx\":\"b\","
            + "\"cy\":\"y\","
            + "\"l\":150,"
            + "\"ex\":\"value\","
            + "\"ey\":\"value\","
            + "\"dot\":1.4,"
            + "\"dim_x\":\"lin\","
            + "\"dim_y\":\"lin\","
            + "\"r\":\"cw\","
            + "\"type\":\"scatterplot\","
            + "\"from_x\":0,"
            + "\"to_x\":0,"
            + "\"from_y\":0,"
            + "\"to_y\":0,"
            + "\"color\":\"ff6a00\"}";

    public static String configJsonWithCCW = "{"
            + "\"name\":\"b (x) vs. y (y)\","
            + "\"cx\":\"b\","
            + "\"cy\":\"y\","
            + "\"l\":150,"
            + "\"ex\":\"value\","
            + "\"ey\":\"value\","
            + "\"dot\":1.4,"
            + "\"dim_x\":\"lin\","
            + "\"dim_y\":\"lin\","
            + "\"r\":\"ccw\","
            + "\"type\":\"scatterplot\","
            + "\"from_x\":0,"
            + "\"to_x\":0,"
            + "\"from_y\":0,"
            + "\"to_y\":0,"
            + "\"color\":\"ff6a00\"}";

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doGet(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testParseConfig() throws JsonParseException, JsonMappingException, IOException {
        GetScatterplotCommand.PlotterConfig config = ParsingUtilities.mapper.readValue(configJson,
                GetScatterplotCommand.PlotterConfig.class);
        Assert.assertEquals("a", config.columnName_x);
        Assert.assertEquals("b", config.columnName_y);
        Assert.assertEquals(ScatterplotFacet.LOG, config.dim_x);
        Assert.assertEquals(ScatterplotFacet.LIN, config.dim_y);
    }

    @Test
    public void testParseConfigWithNone() throws JsonParseException, JsonMappingException, IOException {
        GetScatterplotCommand.PlotterConfig config = ParsingUtilities.mapper.readValue(configJsonWithNone,
                GetScatterplotCommand.PlotterConfig.class);
        Assert.assertEquals(ScatterplotFacet.NO_ROTATION, config.rotation);
    }

    @Test
    public void testParseConfigWithCW() throws JsonParseException, JsonMappingException, IOException {
        GetScatterplotCommand.PlotterConfig config = ParsingUtilities.mapper.readValue(configJsonWithCW,
                GetScatterplotCommand.PlotterConfig.class);
        Assert.assertEquals(ScatterplotFacet.ROTATE_CW, config.rotation);
    }

    @Test
    public void testParseConfigWithCCW() throws JsonParseException, JsonMappingException, IOException {
        GetScatterplotCommand.PlotterConfig config = ParsingUtilities.mapper.readValue(configJsonWithCCW,
                GetScatterplotCommand.PlotterConfig.class);
        Assert.assertEquals(ScatterplotFacet.ROTATE_CCW, config.rotation);
    }

}
