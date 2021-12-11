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
import java.util.Arrays;
import java.util.Collections;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.browsing.facets.RangeFacet.RangeFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
            "          \"type\": \"core/range\",\n" + 
            "          \"columnName\": \"my column\"\n" + 
            "        }";
    
    public static String configWithoutBoundsJson = "{"
    		+ "\"type\":\"range\","
    		+ "\"name\":\"my column\","
    		+ "\"expression\":\"value\","
    		+ "\"columnName\":\"my column\","
    		+ "\"selectNumeric\":true,"
    		+ "\"selectNonNumeric\":true,"
    		+ "\"selectBlank\":true,"
    		+ "\"selectError\":true}";
    
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
    
    public static String facetNoNumericJson = "{"
    		+ "\"baseNumericCount\":0,"
    		+ "\"baseNonNumericCount\":2,"
    		+ "\"baseBlankCount\":0,"
    		+ "\"baseErrorCount\":0,"
    		+ "\"numericCount\":0,"
    		+ "\"nonNumericCount\":2,"
    		+ "\"blankCount\":0,"
    		+ "\"errorCount\":0,"
    		+ "\"name\":\"my column\","
    		+ "\"error\":\"No numeric value present.\","
    		+ "\"expression\":\"value\","
    		+ "\"columnName\":\"my column\"}";
    
    public static String facetSingleNumericJson = "{"
    		+ "\"baseNumericCount\":1,"
    		+ "\"baseNonNumericCount\":1,"
    		+ "\"baseBlankCount\":0,"
    		+ "\"baseErrorCount\":0,"
    		+ "\"numericCount\":1,"
    		+ "\"nonNumericCount\":1,"
    		+ "\"blankCount\":0,"
    		+ "\"errorCount\":0,"
    		+ "\"name\":\"my column\","
    		+ "\"from\":12,"
    		+ "\"baseBins\":[1],"
    		+ "\"to\":13,"
    		+ "\"max\":13,"
    		+ "\"expression\":\"value\","
    		+ "\"step\":1,"
    		+ "\"bins\":[1],"
    		+ "\"columnName\":\"my column\","
    		+ "\"min\":12}";
    
    private static String facetTwoValuesJson = "{"
    		+ "\"baseNumericCount\":2,"
    		+ "\"baseNonNumericCount\":0,"
    		+ "\"baseBlankCount\":0,"
    		+ "\"baseErrorCount\":0,"
    		+ "\"numericCount\":2,"
    		+ "\"nonNumericCount\":0,"
    		+ "\"blankCount\":0,"
    		+ "\"errorCount\":0,"
    		+ "\"name\":\"my column\","
    		+ "\"from\":12,"
    		+ "\"baseBins\":[1,0,0,0,0,0,0,0,0,0,0,0,1],"
    		+ "\"to\":25,"
    		+ "\"max\":25,"
    		+ "\"expression\":\"value\","
    		+ "\"bins\":[1,0,0,0,0,0,0,0,0,0,0,0,1],"
    		+ "\"step\":1,"
    		+ "\"columnName\":\"my column\","
    		+ "\"min\":12}";
    
    private static String listFacetConfigJson = "{"
    		+ "\"type\":\"list\","
    		+ "\"name\":\"other column\","
    		+ "\"columnName\":\"other column\","
    		+ "\"expression\":\"value\","
    		+ "\"omitBlank\":false,"
    		+ "\"omitError\":false,"
    		+ "\"selection\":[{\"v\":{\"v\":\"foo\",\"l\":\"foo\"}}],"
    		+ "\"selectBlank\":false,"
    		+ "\"selectError\":false,"
    		+ "\"invert\":false}";
    
    private static String facetBinBaseJson = "{"
    		+ "\"baseNumericCount\":2,"
    		+ "\"baseNonNumericCount\":0,"
    		+ "\"baseBlankCount\":0,"
    		+ "\"baseErrorCount\":0,"
    		+ "\"numericCount\":1,"
    		+ "\"nonNumericCount\":0,"
    		+ "\"blankCount\":0,"
    		+ "\"errorCount\":0,"
    		+ "\"name\":\"my column\","
    		+ "\"from\":12,"
    		+ "\"baseBins\":[1,0,0,0,0,0,0,0,0,0,0,0,1],"
    		+ "\"to\":25,"
    		+ "\"max\":25,"
    		+ "\"expression\":\"value\","
    		+ "\"bins\":[1,0,0,0,0,0,0,0,0,0,0,0,0],"
    		+ "\"step\":1,"
    		+ "\"columnName\":\"my column\","
    		+ "\"min\":12}";
    
    @BeforeTest
    public void registerFacetConfig() {
    	FacetConfigResolver.registerFacetConfig("core", "range", RangeFacetConfig.class);
    	FacetConfigResolver.registerFacetConfig("core", "list", ListFacetConfig.class);
    	MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }
    
    @Test
    public void serializeRangeFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        RangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, RangeFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeRangeFacet() throws JsonParseException, JsonMappingException, IOException {
        GridState grid = createGrid(
        		new String[] {"my column"},
        		new Serializable[][] {
                { 89.2 },
                { -45.9 },
                { "blah" },
                { 0.4 }
        });

        RangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, RangeFacetConfig.class);
        Engine engine = new Engine(grid, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
        
        TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testNoNumericValue() throws JsonParseException, JsonMappingException, IOException {
    	GridState grid = createGrid(
        		new String[] {"my column"},
        		new Serializable[][] {
                { "blah" },
                { "hey" }
        });
    	
    	RangeFacetConfig config = ParsingUtilities.mapper.readValue(configJson, RangeFacetConfig.class);
    	Engine engine = new Engine(grid, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
    	
    	TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetNoNumericJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testSingleNumericValue() throws JsonParseException, JsonMappingException, IOException {
    	GridState grid = createGrid(
        		new String[] {"my column"},
        		new Serializable[][] {
                { 12 },
                { "hey" }
        });
    	
    	RangeFacetConfig config = ParsingUtilities.mapper.readValue(configWithoutBoundsJson, RangeFacetConfig.class);
    	Engine engine = new Engine(grid, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
    	
    	TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetSingleNumericJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testTwoValues() throws JsonParseException, JsonMappingException, IOException {
    	GridState grid = createGrid(
        		new String[] {"my column", "other column"},
        		new Serializable[][] {
                { 12, "foo" },
                { 24, "bar" }
        });
    	
    	RangeFacetConfig config = ParsingUtilities.mapper.readValue(configWithoutBoundsJson, RangeFacetConfig.class);
    	Engine engine = new Engine(grid, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
    	
    	TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetTwoValuesJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testOtherFacet() throws JsonParseException, JsonMappingException, IOException {
    	GridState grid = createGrid(
        		new String[] {"my column", "other column"},
        		new Serializable[][] {
                { 12, "foo" },
                { 24, "bar" }
        });
    	
    	RangeFacetConfig config = ParsingUtilities.mapper.readValue(configWithoutBoundsJson, RangeFacetConfig.class);
    	ListFacetConfig configListFacet = ParsingUtilities.mapper.readValue(listFacetConfigJson, ListFacetConfig.class);
    	
    	Engine engine = new Engine(grid, new EngineConfig(Arrays.asList(config, configListFacet), Mode.RowBased));
    	
    	TestUtils.isSerializedTo(engine.getFacetResults().get(0), facetBinBaseJson, ParsingUtilities.defaultWriter);
    }
}
