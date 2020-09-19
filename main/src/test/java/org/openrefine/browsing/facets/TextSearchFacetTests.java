/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.browsing.facets;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.facets.TextSearchFacet.TextSearchFacetConfig;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;


public class TextSearchFacetTests extends RefineTest {
    // dependencies
    private GridState grid;
    private List<Row> rows;
    private TextSearchFacetConfig textFilterConfig;
    private TextSearchFacet textFilter;
    private RowFilter rowFilter;
    private String sensitiveConfigJson = "{\"type\":\"core/text\","
            + "\"name\":\"Value\","
            + "\"columnName\":\"Value\","
            + "\"mode\":\"text\","
            + "\"caseSensitive\":true,"
            + "\"invert\":false,"
            + "\"query\":\"A\"}";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() throws IOException, ModelException {
    	FacetConfigResolver.registerFacetConfig("core", "text", TextSearchFacetConfig.class);
        grid = createGrid(
             new String[] {"Value"},
             new Serializable[][] {
            	 {"a"},
            	 {"b"},
            	 {"ab"},
            	 {"Abc"}});
        rows = grid.collectRows().stream().map(ir -> ir.getRow()).collect(Collectors.toList());
    }
    
    private void configureFilter(String filter) throws JsonParseException, JsonMappingException, IOException {
        //Add the facet to the project and create a row filter
        textFilterConfig = ParsingUtilities.mapper.readValue(filter, TextSearchFacetConfig.class);
        textFilter = textFilterConfig.apply(grid.getColumnModel());
        rowFilter = textFilter.getAggregator().getRowFilter();
    }
    
    /**
     * Test to demonstrate the intended behaviour of the function
     */

    @Test
    public void testTextFilter() throws Exception {
        //Apply text filter "a"

        //Column: "Value"
        //Filter Query: "a"
        //Mode: "text"
        //Case sensitive: False
        //Invert: False
        String filter =     "{\"type\":\"core/text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"text\","
                            + "\"caseSensitive\":false,"
                            + "\"invert\":false,"
                            + "\"query\":\"a\"}";
        
        configureFilter(filter);

        //Check each row in the project against the filter
        Assert.assertEquals(rowFilter.filterRow(0, rows.get(0)),true);
        Assert.assertEquals(rowFilter.filterRow(1, rows.get(1)),false);
        Assert.assertEquals(rowFilter.filterRow(2, rows.get(2)),true);
        Assert.assertEquals(rowFilter.filterRow(3, rows.get(3)),true);
    }

    @Test
    public void testInvertedTextFilter() throws Exception {
        //Apply inverted text filter "a"

        //Column: "Value"
        //Filter Query: "a"
        //Mode: "text"
        //Case sensitive: False
        //Invert: True
        String filter =     "{\"type\":\"core/text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"text\","
                            + "\"caseSensitive\":false,"
                            + "\"invert\":true,"
                            + "\"query\":\"a\"}";
        
        configureFilter(filter);

        //Check each row in the project against the filter
        Assert.assertEquals(rowFilter.filterRow(0, rows.get(0)),false);
        Assert.assertEquals(rowFilter.filterRow(1, rows.get(1)),true);
        Assert.assertEquals(rowFilter.filterRow(2, rows.get(2)),false);
        Assert.assertEquals(rowFilter.filterRow(3, rows.get(3)),false);
    }

    @Test
    public void testRegExFilter() throws Exception {
        //Apply regular expression filter "[bc]"

        //Column: "Value"
        //Filter Query: "[bc]"
        //Mode: "regex"
        //Case sensitive: False
        //Invert: False
        String filter =     "{\"type\":\"core/text\","
                            + "\"name\":\"Value\","
                            + "\"columnName\":\"Value\","
                            + "\"mode\":\"regex\","
                            + "\"caseSensitive\":false,"
                            + "\"invert\":false,"
                            + "\"query\":\"[bc]\"}";
        
        configureFilter(filter);

        //Check each row in the project against the filter
        Assert.assertEquals(rowFilter.filterRow(0, rows.get(0)),false);
        Assert.assertEquals(rowFilter.filterRow(1, rows.get(1)),true);
        Assert.assertEquals(rowFilter.filterRow(2, rows.get(2)),true);
        Assert.assertEquals(rowFilter.filterRow(3, rows.get(3)),true);
    }

    @Test
    public void testCaseSensitiveFilter() throws Exception {
        //Apply case-sensitive filter "A"
        
        configureFilter(sensitiveConfigJson);

        //Check each row in the project against the filter
        //Expect to retrieve one row containing "Abc"
        Assert.assertEquals(rowFilter.filterRow(0, rows.get(0)),false);
        Assert.assertEquals(rowFilter.filterRow(1, rows.get(1)),false);
        Assert.assertEquals(rowFilter.filterRow(2, rows.get(2)),false);
        Assert.assertEquals(rowFilter.filterRow(3, rows.get(3)),true);
    }
    
    @Test
    public void serializeTextSearchFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        TextSearchFacetConfig config = ParsingUtilities.mapper.readValue(sensitiveConfigJson, TextSearchFacetConfig.class);
        TestUtils.isSerializedTo(config, sensitiveConfigJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeTextSearchFacet() throws JsonParseException, JsonMappingException, IOException {
        TextSearchFacetConfig config = ParsingUtilities.mapper.readValue(sensitiveConfigJson, TextSearchFacetConfig.class);
        Engine engine = new Engine(grid, new EngineConfig(Collections.singletonList(config), Mode.RowBased));
    	
    	TestUtils.isSerializedTo(engine.getFacetResults().get(0), sensitiveConfigJson, ParsingUtilities.defaultWriter);
    }
}

