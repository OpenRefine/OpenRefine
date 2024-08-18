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

package com.google.refine.browsing.facets;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

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

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @Test
    public void serializeScatterplotFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);
        TestUtils.isSerializedTo(config, configJson);
    }

    @Test
    public void serializeScatterplotFacet() throws JsonParseException, JsonMappingException, IOException {
        Project project = createProject(
                new String[] { "my column", "e" },
                new Serializable[][] {
                        { 89.2, 89.2 },
                        { -45.9, -45.9 },
                        { "blah", "blah" },
                        { 0.4, 0.4 }
                });
        Engine engine = new Engine(project);

        ScatterplotFacetConfig config = ParsingUtilities.mapper.readValue(configJson, ScatterplotFacetConfig.class);

        ScatterplotFacet facet = config.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        TestUtils.isSerializedTo(facet, facetJson);

        RowFilter filter = facet.getRowFilter(project);
        assertTrue(filter.filterRow(project, 0, project.rows.get(0)));
        assertFalse(filter.filterRow(project, 1, project.rows.get(1)));
        assertTrue(filter.filterRow(project, 3, project.rows.get(3)));
    }
}
