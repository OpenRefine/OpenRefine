package com.google.refine.tests.browsing.facets;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.ScatterplotFacet;
import com.google.refine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ScatterplotFacetTests extends RefineTest {
    public static String configJson = "{\n" + 
            "          \"to_x\": 1,\n" + 
            "          \"to_y\": 1,\n" + 
            "          \"dot\": 1,\n" + 
            "          \"from_x\": 0.21333333333333335,\n" + 
            "          \"l\": 150,\n" + 
            "          \"type\": \"scatterplot\",\n" + 
            "          \"from_y\": 0.26666666666666666,\n" + 
            "          \"dim_y\": \"lin\",\n" + 
            "          \"ex\": \"value\",\n" + 
            "          \"dim_x\": \"lin\",\n" +
            "          \"ey\": \"value\",\n" + 
            "          \"cx\": \"my column\",\n" + 
            "          \"cy\": \"e\",\n" + 
            "          \"name\": \"my column (x) vs. e (y)\"\n" + 
            "        }";
    
    public static String facetJson = "{"
            + "\"name\":\"my column (x) vs. e (y)\","
            + "\"cx\":\"my column\","
            + "\"ex\":\"value\","
            + "\"cy\":\"e\","
            + "\"ey\":\"value\","
            + "\"l\":150,"
            + "\"dot\":1,"
            + "\"r\":0,"
            + "\"dim_x\":0,"
            + "\"dim_y\":0,"
            + "\"color\":\"000000\","
            + "\"from_x\":0.21333333333333335,"
            + "\"to_x\":1,"
            + "\"from_y\":0.26666666666666666,"
            + "\"to_y\":1"
            + "}";
    
    @Test
    public void serializeScatterplotFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void serializeScatterplotFacet() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("my column,e\n"
                + "89.2,89.2\n" + 
                "-45.9,-45.9\n" + 
                "blah,blah\n" + 
                "0.4,0.4\n");
        Engine engine = new Engine(project);
        project.rows.get(0).cells.set(0, new Cell(89.2, null));
        project.rows.get(0).cells.set(1, new Cell(89.2, null));
        project.rows.get(1).cells.set(0, new Cell(-45.9, null));
        project.rows.get(1).cells.set(1, new Cell(-45.9, null));
        project.rows.get(3).cells.set(0, new Cell(0.4, null));
        project.rows.get(3).cells.set(1, new Cell(0.4, null));
        
        ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
        
        ScatterplotFacet facet = config.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        
        TestUtils.isSerializedTo(facet, facetJson);
    }
}
