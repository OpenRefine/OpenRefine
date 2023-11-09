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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
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

    private static String jsonFacetError = "{"
            + "\"name\":\"facet A\","
            + "\"expression\":\"value+\\\"bar\\\"\","
            + "\"columnName\":\"Column A\","
            + "\"invert\":false,"
            + "\"error\":\"No column named Column A\"" +
            "}";

    private static String jsonFacet = "{"
            + "\"name\":\"facet A\","
            + "\"expression\":\"value+\\\"bar\\\"\","
            + "\"columnName\":\"Column A\","
            + "\"invert\":false,"
            + "\"choices\":["
            + "     {\"v\":{\"v\":\"foobar\",\"l\":\"foobar\"},\"c\":1,\"s\":true},"
            + "     {\"v\":{\"v\":\"barbar\",\"l\":\"barbar\"},\"c\":1,\"s\":false}"
            + "]}";

    private static String selectedEmptyChoiceFacet = "{"
            + "\"name\":\"facet A\","
            + "\"expression\":\"value+\\\"bar\\\"\","
            + "\"columnName\":\"Column A\","
            + "\"invert\":false,"
            + "\"choices\":["
            + "    {\"v\":{\"v\":\"ebar\",\"l\":\"ebar\"},\"c\":1,\"s\":false},"
            + "    {\"v\":{\"v\":\"cbar\",\"l\":\"cbar\"},\"c\":1,\"s\":false},"
            + "    {\"v\":{\"v\":\"abar\",\"l\":\"abar\"},\"c\":1,\"s\":false},"
            + "    {\"v\":{\"v\":\"foobar\",\"l\":\"true\"},\"c\":0,\"s\":true}"
            + "]}";

    @BeforeTest
    public void registerFacetConfig() {
        FacetConfigResolver.registerFacetConfig("core", "list", ListFacetConfig.class);
    }

    @Test
    public void serializeListFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        TestUtils.isSerializedTo(facetConfig, jsonConfig, ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeListFacet() throws JsonParseException, JsonMappingException, IOException {
        Project project = createProject(new String[] { "Column A" },
                new Serializable[] { "foo",
                        "bar" });
        Engine engine = new Engine(project);

        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);

        Facet facet = facetConfig.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());

        TestUtils.isSerializedTo(facet, jsonFacet, ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeListFacetWithError() throws JsonParseException, JsonMappingException, IOException {
        Project project = createProject(new String[] { "other column" },
                new Serializable[] {
                        "foo",
                        "bar" });

        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        TestUtils.isSerializedTo(facet, jsonFacetError, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testSelectedEmptyChoice() throws IOException {
        Project project = createProject(new String[] {
                "Column A" },
                new Serializable[] {
                        "a",
                        "c",
                        "e" });
        Engine engine = new Engine(project);

        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        TestUtils.isSerializedTo(facet, selectedEmptyChoiceFacet, ParsingUtilities.defaultWriter);
    }
}
