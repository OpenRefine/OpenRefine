package org.openrefine.browsing.facets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openrefine.ProjectManager;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.browsing.util.StringValuesFacetState;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.preference.PreferenceStore;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ListFacetResultTests {
	
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
    		+ "    {\"v\":{\"v\":\"foobar\",\"l\":\"foobar\"},\"c\":0,\"s\":true}"
    		+ "]}";
    
    private ListFacetConfig config;
    
    @BeforeMethod
    public void setUpConfig() {
    	MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    	ProjectManager.singleton = mock(ProjectManager.class);
    	when(ProjectManager.singleton.getPreferenceStore()).thenReturn(new PreferenceStore());
    	
    	config = new ListFacetConfig();
    	config.name = "facet A";
    	config.setExpression("value+\"bar\"");
    	config.invert = false;
    	config.selectBlank = false;
    	config.selectError = false;
    	config.omitBlank = false;
    	config.omitError = false;
    	config.columnName = "Column A";
    	config.selection = Arrays.asList(new DecoratedValue("foobar", "foobar"));
    }
    
    @Test
    public void serializeListFacet() throws JsonParseException, JsonMappingException, IOException {
    	ColumnModel model = new ColumnModel(Arrays.asList(new ColumnMetadata("Column A")));
    	ListFacet facet = config.apply(model);
    	
    	Map<String,Long> map = new HashMap<>();
    	map.put("foobar", 1L);
    	map.put("barbar", 1L);
    	StringValuesFacetState state = new StringValuesFacetState(map, 0L, 0L);

        ListFacetResult result = facet.getFacetResult(state);
        
        TestUtils.isSerializedTo(result, jsonFacet, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeListFacetWithError() throws JsonParseException, JsonMappingException, IOException {
        ColumnModel model = new ColumnModel(Arrays.asList(new ColumnMetadata("Column B")));
    	ListFacet facet = config.apply(model);
    	
    	ListFacetResult result = facet.getFacetResult(facet.getInitialFacetState());
       
        TestUtils.isSerializedTo(result, jsonFacetError, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testSelectedEmptyChoice() throws IOException {
    	ColumnModel model = new ColumnModel(Arrays.asList(new ColumnMetadata("Column A")));
    	ListFacet facet = config.apply(model);
    	
    	Map<String,Long> map = new HashMap<>();
    	map.put("abar", 1L);
    	map.put("cbar", 1L);
    	map.put("ebar", 1L);
    	StringValuesFacetState state = new StringValuesFacetState(map, 0L, 0L);

    	ListFacetResult result = facet.getFacetResult(state);
    	
    	TestUtils.isSerializedTo(result, selectedEmptyChoiceFacet, ParsingUtilities.defaultWriter);
    }
}
