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

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.RangeFacet;
import com.google.refine.browsing.facets.RangeFacet.RangeFacetConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

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
