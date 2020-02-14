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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ListFacetTests extends RefineTest {

    private static String jsonConfig = "{"
            + "\"type\":\"core/list\","
            + "\"name\":\"facet A\","
            + "\"columnName\":\"Column A\","
            + "\"expression\":\"value+\\\"bar\\\"\","
            + "\"omitBlank\":false,"
            + "\"omitError\":false,"
            + "\"selection\":[{\"v\":{\"v\":\"foobar\",\"l\":\"true\"}}],"
            + "\"selectBlank\":false,"
            + "\"selectError\":false,"
            + "\"invert\":false"
            + "}";

    private static String expectedFacets = "[ {\n" +
            "       \"choices\" : [ {\n" +
            "         \"c\" : 1,\n" +
            "         \"s\" : false,\n" +
            "         \"v\" : {\n" +
            "           \"l\" : \"a\",\n" +
            "           \"v\" : \"a\"\n" +
            "         }\n" +
            "       }, {\n" +
            "         \"c\" : 1,\n" +
            "         \"s\" : false,\n" +
            "         \"v\" : {\n" +
            "           \"l\" : \"c\",\n" +
            "           \"v\" : \"c\"\n" +
            "         }\n" +
            "       } ],\n" +
            "       \"columnName\" : \"foo\",\n" +
            "       \"expression\" : \"grel:value\",\n" +
            "       \"invert\" : false,\n" +
            "       \"name\" : \"foo\"\n" +
            "     }, {\n" +
            "       \"choices\" : [ {\n" +
            "         \"c\" : 1,\n" +
            "         \"s\" : false,\n" +
            "         \"v\" : {\n" +
            "           \"l\" : \"b\",\n" +
            "           \"v\" : \"b\"\n" +
            "         }\n" +
            "       }, {\n" +
            "         \"c\" : 1,\n" +
            "         \"s\" : false,\n" +
            "         \"v\" : {\n" +
            "           \"l\" : \"d\",\n" +
            "           \"v\" : \"d\"\n" +
            "         }\n" +
            "       } ],\n" +
            "       \"columnName\" : \"bar\",\n" +
            "       \"expression\" : \"grel:value\",\n" +
            "       \"invert\" : false,\n" +
            "       \"name\" : \"foo\"\n" +
            "     } ]";

    @BeforeTest
    public void registerFacetConfig() {
        FacetConfigResolver.registerFacetConfig("core", "list", ListFacetConfig.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @Test
    public void serializeListFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        TestUtils.isSerializedTo(facetConfig, jsonConfig, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testTwoListFacets() {
        String expectedJson = "[ {\n" +
                "       \"choices\" : [ {\n" +
                "         \"c\" : 1,\n" +
                "         \"s\" : false,\n" +
                "         \"v\" : {\n" +
                "           \"l\" : \"a\",\n" +
                "           \"v\" : \"a\"\n" +
                "         }\n" +
                "       }, {\n" +
                "         \"c\" : 1,\n" +
                "         \"s\" : false,\n" +
                "         \"v\" : {\n" +
                "           \"l\" : \"c\",\n" +
                "           \"v\" : \"c\"\n" +
                "         }\n" +
                "       } ],\n" +
                "       \"columnName\" : \"foo\",\n" +
                "       \"expression\" : \"grel:value\",\n" +
                "       \"invert\" : false,\n" +
                "       \"name\" : \"foo\"\n" +
                "     }, {\n" +
                "       \"choices\" : [ {\n" +
                "         \"c\" : 1,\n" +
                "         \"s\" : false,\n" +
                "         \"v\" : {\n" +
                "           \"l\" : \"b\",\n" +
                "           \"v\" : \"b\"\n" +
                "         }\n" +
                "       }, {\n" +
                "         \"c\" : 1,\n" +
                "         \"s\" : false,\n" +
                "         \"v\" : {\n" +
                "           \"l\" : \"d\",\n" +
                "           \"v\" : \"d\"\n" +
                "         }\n" +
                "       } ],\n" +
                "       \"columnName\" : \"bar\",\n" +
                "       \"expression\" : \"grel:value\",\n" +
                "       \"invert\" : false,\n" +
                "       \"name\" : \"foo\"\n" +
                "     } ]";

        Project project = createProject("my project",
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" }
                });
        ListFacetConfig firstColumn = new ListFacetConfig();
        firstColumn.columnName = "foo";
        firstColumn.expression = "grel:value";
        firstColumn.invert = false;
        firstColumn.name = "foo";
        firstColumn.omitBlank = false;
        firstColumn.omitError = false;
        firstColumn.selectBlank = false;
        firstColumn.selectError = false;
        firstColumn.selection = Collections.emptyList();
        ListFacetConfig secondColumn = new ListFacetConfig();
        secondColumn.columnName = "bar";
        secondColumn.expression = "grel:value";
        secondColumn.invert = false;
        secondColumn.name = "foo";
        secondColumn.omitBlank = false;
        secondColumn.omitError = false;
        secondColumn.selectBlank = false;
        secondColumn.selectError = false;
        secondColumn.selection = Collections.emptyList();
        EngineConfig config = new EngineConfig(Arrays.asList(firstColumn, secondColumn), Engine.Mode.RowBased);
        Engine engine = new Engine(project.getCurrentGridState(), config);
        TestUtils.isSerializedTo(engine.getFacetResults(), expectedJson, ParsingUtilities.defaultWriter);
    }

}
