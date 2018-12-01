package com.google.refine.tests.browsing.facets;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.TimeRangeFacet;
import com.google.refine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;


public class TimeRangeFacetTests extends RefineTest {
    
    public static String facetJson = "{"
            + "\"name\":\"my column\","
            + "\"expression\":\"value\","
            + "\"columnName\":\"my column\","
            + "\"min\":1199329445000,"
            + "\"max\":1514966950000,"
            + "\"step\":31556952000,"
            + "\"bins\":[1,0,0,0,1,0,0,0,0,0,1],"
            + "\"baseBins\":[1,0,0,0,1,0,0,0,0,0,1],"
            + "\"from\":1262443349000,"
            + "\"to\":1514966950000,"
            + "\"baseTimeCount\":3,"
            + "\"baseNonTimeCount\":1,"
            + "\"baseBlankCount\":0,"
            + "\"baseErrorCount\":0,"
            + "\"timeCount\":3,"
            + "\"nonTimeCount\":1,"
            + "\"blankCount\":0,"
            + "\"errorCount\":0}";
    
    public static String configJson = "{\n" + 
            "          \"selectNonTime\": true,\n" + 
            "          \"expression\": \"value\",\n" + 
            "          \"selectBlank\": true,\n" + 
            "          \"selectError\": true,\n" + 
            "          \"selectTime\": true,\n" + 
            "          \"name\": \"my column\",\n" + 
            "          \"from\": 1262443349000,\n" + 
            "          \"to\": 1514966950000,\n" + 
            "          \"type\": \"timerange\",\n" + 
            "          \"columnName\": \"my column\"\n" + 
            "        }";
    
    @Test
    public void serializeTimeRangeFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        TimeRangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, TimeRangeFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson);
    }
    
    @Test
    public void serializeTimeRangeFacet() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("my column\n"
                + "placeholder\n"
                + "nontime\n"
                + "placeholder\n"
                + "placeholder\n");
        project.rows.get(0).cells.set(0, new Cell(OffsetDateTime.parse("2018-01-03T08:09:10Z"), null));
        project.rows.get(2).cells.set(0, new Cell(OffsetDateTime.parse("2008-01-03T03:04:05Z"), null));
        project.rows.get(3).cells.set(0, new Cell(OffsetDateTime.parse("2012-04-05T02:00:01Z"), null));
        
        Engine engine = new Engine(project);
        TimeRangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, TimeRangeFacetConfig.class);
        TimeRangeFacet facet = config.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        TestUtils.isSerializedTo(facet, facetJson);
    }
}
