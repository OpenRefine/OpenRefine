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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.model.RowFilter;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ScatterplotFacetTests extends RefineTest {
    public static String configJson = "{\n" + 
            "          \"max_x\" : 89.2," +
            "          \"max_y\" : 32," +
            "          \"min_x\" : -45.9," +
            "          \"min_y\" : -38," +
            "          \"to_x\": 1,\n" + 
            "          \"to_y\": 1,\n" + 
            "          \"dot\": 1,\n" + 
            "          \"from_x\": 0.2,\n" + 
            "          \"l\": 150,\n" + 
            "          \"type\": \"core/scatterplot\",\n" + 
            "          \"from_y\": 0.25,\n" + 
            "          \"dim_y\": \"lin\",\n" + 
            "          \"ex\": \"value\",\n" + 
            "          \"dim_x\": \"lin\",\n" +
            "          \"ey\": \"value\",\n" + 
            "          \"cx\": \"my column\",\n" + 
            "          \"cy\": \"e\",\n" +
            "          \"r\": \"none\"," + 
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
            + "\"r\":\"none\","
            + "\"dim_x\":\"lin\","
            + "\"dim_y\":\"lin\","
            + "\"from_x\":0.2,"
            + "\"to_x\":1,"
            + "\"from_y\":0.25,"
            + "\"to_y\":1,"
            + "\"max_x\" : 89.2,"
            + "\"max_y\" : 32,"
            + "\"min_x\" : -45.9,"
            + "\"min_y\" : -38"
            + "}";
    
    public static String facetWithErrorJson = "{"
            + "\"name\":\"my column (x) vs. e (y)\","
            + "\"cx\":\"my column\","
            + "\"ex\":\"value\","
            + "\"cy\":\"e\","
            + "\"ey\":\"value\","
            + "\"error_x\" : \"No column named my column\","
            + "\"error_y\" : \"No column named e\","
            + "\"l\":150,"
            + "\"dot\":1,"
            + "\"r\":\"none\","
            + "\"dim_x\":\"lin\","
            + "\"dim_y\":\"lin\""
            + "}";
    
    GridState grid;
    
    @BeforeTest
    public void registerFacetConfig() {
    	MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    	FacetConfigResolver.registerFacetConfig("core", "scatterplot", ScatterplotFacetConfig.class);
    	grid = createGrid(new String[] {"my column","e"},
        		new Serializable[][] {
                { 89.2, 32 },
                { -45.9, -38 }, 
                { "blah","blah" },
                { 0.4, 1 }});
    }
    
    @Test
    public void serializeScatterplotFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeScatterplotFacetResult() throws JsonParseException, JsonMappingException, IOException {
        ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
        EngineConfig engineConfig = new EngineConfig(Collections.singletonList(config), Mode.RowBased);
        Engine engine = new Engine(grid, engineConfig);
        
        TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeFacetWithError() throws JsonParseException, JsonMappingException, IOException {
    	GridState grid = createGrid(new String[] { "foo" },
    			new Serializable[][] {
    		{ "bar" }
    	});
    	
    	ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
        EngineConfig engineConfig = new EngineConfig(Collections.singletonList(config), Mode.RowBased);
        Engine engine = new Engine(grid, engineConfig);
        
        TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetWithErrorJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testFilterRows() throws JsonParseException, JsonMappingException, IOException {
    	ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
    	ScatterplotFacet facet = config.apply(grid.getColumnModel());
        RowFilter filter = facet.getAggregator().getRowFilter();
        
        assertTrue(filter.filterRow(0, grid.getRow(0)));
        assertFalse(filter.filterRow(1, grid.getRow(1)));
        assertFalse(filter.filterRow(2, grid.getRow(2)));
        assertTrue(filter.filterRow(3, grid.getRow(3)));
    }
}
