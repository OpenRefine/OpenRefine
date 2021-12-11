/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.browsing.facets;

import java.io.IOException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collections;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;


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
    
    public static String facetNoTimeJson = "{"
            + "\"name\":\"my column\","
            + "\"expression\":\"value\","
            + "\"columnName\":\"my column\","
            + "\"min\":0,"
            + "\"max\":1,"
            + "\"step\":1,"
            + "\"bins\":[0],"
            + "\"baseBins\":[0],"
            + "\"from\":1262443349000,"
            + "\"to\":1514966950000,"
            + "\"baseTimeCount\":0,"
            + "\"baseNonTimeCount\":3,"
            + "\"baseBlankCount\":1,"
            + "\"baseErrorCount\":0,"
            + "\"timeCount\":0,"
            + "\"nonTimeCount\":3,"
            + "\"blankCount\":1,"
            + "\"errorCount\":0}";
    
    public static String facetNoColumnJson = "{"
    		+ "\"error\":\"No column named my column\","
            + "\"name\":\"my column\","
            + "\"expression\":\"value\","
            + "\"columnName\":\"my column\","
            + "\"step\":1,"
            + "\"baseTimeCount\":0,"
            + "\"baseNonTimeCount\":0,"
            + "\"baseBlankCount\":0,"
            + "\"baseErrorCount\":0,"
            + "\"timeCount\":0,"
            + "\"nonTimeCount\":0,"
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
            "          \"type\": \"core/timerange\",\n" + 
            "          \"columnName\": \"my column\"\n" + 
            "        }";
    
    @BeforeTest
    public void registerFacetConfig() {
    	FacetConfigResolver.registerFacetConfig("core", "timerange", TimeRangeFacetConfig.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }
    
    @Test
    public void serializeTimeRangeFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        TimeRangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, TimeRangeFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeTimeRangeFacet() throws JsonParseException, JsonMappingException, IOException {
        GridState project = createGrid(new String[] {"my column"},
        		new Serializable[][] {
                { OffsetDateTime.parse("2018-01-03T08:09:10Z") },
                { "nontime" },
                { OffsetDateTime.parse("2008-01-03T03:04:05Z") },
                { OffsetDateTime.parse("2012-04-05T02:00:01Z") }});
        
        TimeRangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, TimeRangeFacetConfig.class);
        Engine engine = new Engine(project, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
        
        TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeTimeRangeFacetMismatchingColumn() throws JsonParseException, JsonMappingException, IOException {
        GridState project = createGrid(new String[] {"my mismatching column"},
        		new Serializable[][] {
                { OffsetDateTime.parse("2018-01-03T08:09:10Z") },
                { "nontime" },
                { OffsetDateTime.parse("2008-01-03T03:04:05Z") },
                { OffsetDateTime.parse("2012-04-05T02:00:01Z") }});
        
        TimeRangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, TimeRangeFacetConfig.class);
        Engine engine = new Engine(project, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
        
        TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetNoColumnJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeTimeRangeFacetNoTimeValue() throws JsonParseException, JsonMappingException, IOException {
        GridState project = createGrid(new String[] {"my column"},
        		new Serializable[][] {
                { null },
                { "nontime" },
                { 1234L },
                { "foo" }});
        
        TimeRangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, TimeRangeFacetConfig.class);
        Engine engine = new Engine(project, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
        
        TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetNoTimeJson, ParsingUtilities.defaultWriter);
    }
}
