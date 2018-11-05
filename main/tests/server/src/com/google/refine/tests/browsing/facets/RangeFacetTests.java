package com.google.refine.tests.browsing.facets;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.RangeFacet;
import com.google.refine.browsing.facets.RangeFacet.RangeFacetConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class RangeFacetTests extends RefineTest {
    public static String configJson = "{\n" + 
            "          \"selectNumeric\": true,\n" + 
            "          \"expression\": \"value\",\n" + 
            "          \"selectBlank\": true,\n" + 
            "          \"selectNonNumeric\": true,\n" + 
            "          \"selectError\": true,\n" + 
            "          \"name\": \"my column\",\n" + 
            "          \"from\": -30,\n" + 
            "          \"to\": 90,\n" + 
            "          \"type\": \"range\",\n" + 
            "          \"columnName\": \"my column\"\n" + 
            "        }";
    
    public static String facetJson = "{"
            + "\"name\":\"my column\","
            + "\"expression\":\"value\","
            + "\"columnName\":\"my column\","
            + "\"min\":-50,"
            + "\"max\":90,"
            + "\"step\":10,"
            + "\"bins\":[1,0,0,0,0,1,0,0,0,0,0,0,0,1],"
            + "\"baseBins\":[1,0,0,0,0,1,0,0,0,0,0,0,0,1],"
            + "\"from\":-30,"
            + "\"to\":90,"
            + "\"baseNumericCount\":3,"
            + "\"baseNonNumericCount\":1,"
            + "\"baseBlankCount\":0,"
            + "\"baseErrorCount\":0,"
            + "\"numericCount\":3,"
            + "\"nonNumericCount\":1,"
            + "\"blankCount\":0,"
            + "\"errorCount\":0}";
    
    @Test
    public void serializeRangeFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        RangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, RangeFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void serializeRangeFacet() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("my column\n"
                + "89.2\n"
                + "-45.9\n"
                + "blah\n"
                + "0.4\n");
        project.rows.get(0).cells.set(0, new Cell(89.2, null));
        project.rows.get(1).cells.set(0, new Cell(-45.9, null));
        project.rows.get(3).cells.set(0, new Cell(0.4, null));
        Engine engine = new Engine(project);
        RangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, RangeFacetConfig.class);
        RangeFacet facet = config.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        TestUtils.isSerializedTo(facet, facetJson);
    }
}
