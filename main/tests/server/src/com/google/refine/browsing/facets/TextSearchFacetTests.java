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

package com.google.refine.browsing.facets;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.RefineTest;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.TextSearchFacet;
import com.google.refine.browsing.facets.TextSearchFacet.TextSearchFacetConfig;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class TextSearchFacetTests extends RefineTest {

    // dependencies
    private Project project;
    private TextSearchFacetConfig textfilterconfig;
    private TextSearchFacet textfilter;
    private RowFilter rowfilter;
    private String sensitiveConfigJson = "{\"type\":\"text\","
            + "\"name\":\"Value\","
            + "\"columnName\":\"Value\","
            + "\"mode\":\"text\","
            + "\"caseSensitive\":true,"
            + "\"invert\":false,"
            + "\"query\":\"A\"}";

    private String sensitiveFacetJson = "{\"name\":\"Value\","
            + "\"columnName\":\"Value\","
            + "\"query\":\"A\","
            + "\"mode\":\"text\","
            + "\"caseSensitive\":true,"
            + "\"invert\":false}";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() throws IOException, ModelException {
        project = createCSVProject("TextSearchFacet",
                "Value\n"
                        + "a\n"
                        + "b\n"
                        + "ab\n"
                        + "Abc\n");
    }

    private void configureFilter(String filter) throws JsonParseException, JsonMappingException, IOException {
        // Add the facet to the project and create a row filter
        textfilterconfig = ParsingUtilities.mapper.readValue(filter, TextSearchFacetConfig.class);
        textfilter = textfilterconfig.apply(project);
        rowfilter = textfilter.getRowFilter(project);
    }

    /**
     * Test to demonstrate the intended behaviour of the function
     */

    @Test
    public void testTextFilter() throws Exception {
        // Apply text filter "a"

        // Column: "Value"
        // Filter Query: "a"
        // Mode: "text"
        // Case sensitive: False
        // Invert: False
        String filter = "{\"type\":\"text\","
                + "\"name\":\"Value\","
                + "\"columnName\":\"Value\","
                + "\"mode\":\"text\","
                + "\"caseSensitive\":false,"
                + "\"invert\":false,"
                + "\"query\":\"a\"}";

        configureFilter(filter);

        // Check each row in the project against the filter
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)), true);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)), true);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)), true);
    }

    @Test
    public void testInvertedTextFilter() throws Exception {
        // Apply inverted text filter "a"

        // Column: "Value"
        // Filter Query: "a"
        // Mode: "text"
        // Case sensitive: False
        // Invert: True
        String filter = "{\"type\":\"text\","
                + "\"name\":\"Value\","
                + "\"columnName\":\"Value\","
                + "\"mode\":\"text\","
                + "\"caseSensitive\":false,"
                + "\"invert\":true,"
                + "\"query\":\"a\"}";

        configureFilter(filter);

        // Check each row in the project against the filter
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)), true);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)), false);
    }

    @Test
    public void testRegExFilter() throws Exception {
        // Apply regular expression filter "[bc]"

        // Column: "Value"
        // Filter Query: "[bc]"
        // Mode: "regex"
        // Case sensitive: False
        // Invert: False
        String filter = "{\"type\":\"text\","
                + "\"name\":\"Value\","
                + "\"columnName\":\"Value\","
                + "\"mode\":\"regex\","
                + "\"caseSensitive\":false,"
                + "\"invert\":false,"
                + "\"query\":\"[bc]\"}";

        configureFilter(filter);

        // Check each row in the project against the filter
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)), true);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)), true);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)), true);
    }

    @Test
    public void testCaseSensitiveFilter() throws Exception {
        // Apply case-sensitive filter "A"

        configureFilter(sensitiveConfigJson);

        // Check each row in the project against the filter
        // Expect to retrieve one row containing "Abc"
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)), false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)), true);
    }

    @Test
    public void serializeTextSearchFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        TextSearchFacetConfig config = ParsingUtilities.mapper.readValue(sensitiveConfigJson, TextSearchFacetConfig.class);
        TestUtils.isSerializedTo(config, sensitiveConfigJson);
    }

    @Test
    public void serializeTextSearchFacet() throws JsonParseException, JsonMappingException, IOException {
        TextSearchFacetConfig config = ParsingUtilities.mapper.readValue(sensitiveConfigJson, TextSearchFacetConfig.class);
        TextSearchFacet facet = config.apply(project);
        TestUtils.isSerializedTo(facet, sensitiveFacetJson);
    }
}
