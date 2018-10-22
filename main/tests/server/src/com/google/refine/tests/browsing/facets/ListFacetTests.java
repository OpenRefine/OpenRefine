package com.google.refine.tests.browsing.facets;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class ListFacetTests extends RefineTest {
    
    private static String jsonConfig = "{"
            + "\"type\":\"list\","
            + "\"name\":\"facet A\","
            + "\"expression\":\"value+\\\"bar\\\"\","
            + "\"columnName\":\"Column A\","
            + "\"omitBlank\":false,"
            + "\"omitError\":false,"
            + "\"selection\":[{\"v\":{\"v\":\"foobar\",\"l\":\"true\"}}],"
            + "\"selectNumber\":false,"
            + "\"selectDateTime\":false,"
            + "\"selectBoolean\":false,"
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

    @Test
    public void serializeListFacetConfig() throws JsonParseException, JsonMappingException, IOException {
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        TestUtils.isSerializedTo(facetConfig, jsonConfig);
    }
    
    @Test
    public void serializeListFacet() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("Column A\n" +
                "foo\n" +
                "bar\n");
        Engine engine = new Engine(project);
        
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        
        Facet facet = facetConfig.apply(project);
        facet.computeChoices(project, engine.getAllFilteredRows());
        
        TestUtils.isSerializedTo(facet, jsonFacet);
    }
    
    @Test
    public void serializeListFacetWithError() throws JsonParseException, JsonMappingException, IOException {
        Project project = createCSVProject("other column\n" +
                "foo\n" +
                "bar\n");
       
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        TestUtils.isSerializedTo(facet, jsonFacetError);
    }
    
    @Test
    public void testSelectedEmptyChoice() {
    	Project project = createCSVProject("Column A\n" + 
    			"a\n" + 
    			"c\n" + 
    			"e");
    	Engine engine = new Engine(project);
    	
    	ListFacetConfig facetConfig = new ListFacetConfig();
    	facetConfig.initializeFromJSON(new JSONObject(jsonConfig));
    	Facet facet = facetConfig.apply(project);
    	facet.computeChoices(project, engine.getAllFilteredRows());
    	TestUtils.isSerializedTo(facet, selectedEmptyChoiceFacet);
    }
}
