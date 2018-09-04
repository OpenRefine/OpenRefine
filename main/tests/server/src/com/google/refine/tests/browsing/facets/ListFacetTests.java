package com.google.refine.tests.browsing.facets;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ListFacetTests extends RefineTest {
    
    private static String jsonConfig = "{"
            + "\"type\":\"list\","
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
            + "\"error\":\"No column named Column A\"}\" are not equal as JSON strings.\n" + 
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

    @Test
    public void serializeListFacetConfig() {
        ListFacetConfig facetConfig = new ListFacetConfig();
        facetConfig.initializeFromJSON(new JSONObject(jsonConfig));
        TestUtils.isSerializedTo(facetConfig, jsonConfig);
    }
    
    @Test
    public void serializeListFacet() {
        Project project = createCSVProject("Column A\n" +
                "foo\n" +
                "bar\n");
        Engine engine = new Engine(project);
        
        ListFacetConfig facetConfig = new ListFacetConfig();
        facetConfig.initializeFromJSON(new JSONObject(jsonConfig));
        
        Facet facet = facetConfig.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        
        TestUtils.isSerializedTo(facet, jsonFacet);
    }
    
    @Test
    public void serializeListFacetWithError() {
        Project project = createCSVProject("other column\n" +
                "foo\n" +
                "bar\n");
       
        ListFacetConfig facetConfig = new ListFacetConfig();
        facetConfig.initializeFromJSON(new JSONObject(jsonConfig));
        Facet facet = facetConfig.apply(project);
        TestUtils.isSerializedTo(facet, jsonFacetError);
    }
}
