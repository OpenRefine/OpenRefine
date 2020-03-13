package org.openrefine.history.dag;

import static org.openrefine.history.dag.DagSliceTestHelper.columns;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ReorderSliceTests {
    
    public static final String json = "{\n" + 
            "       \"renames\" : {\n" + 
            "         \"col2\" : \"renamed\"\n" + 
            "       },\n" + 
            "       \"reorderedColumns\" : [ \"col3\", \"col2\", \"col5\", \"col0\" ],\n" + 
            "       \"type\" : \"reorder\"\n" + 
            "     }";
    
    public static final String jsonReorder = "{\n" + 
            "       \"reorderedColumns\" : [ \"col3\", \"col2\", \"col5\", \"col0\" ],\n" + 
            "       \"type\" : \"reorder\"\n" + 
            "     }";
    
    private ReorderSlice SUT;
    
    @BeforeMethod
    public void setUp() {
        SUT = new ReorderSlice(
                Arrays.asList("col3", "col2", "col5", "col0"),
                Collections.singletonMap("col2", "renamed"));
    }
    
    @Test
    public void testApplyToColumns() throws IncompatibleSliceException {
        ColumnModel input = columns("col0", "col1", "col2", "col3", "col4", "col5");
        ColumnModel expected = columns(
                new ColumnMetadata("col3"),
                new ColumnMetadata("col2", "renamed", null, null),
                new ColumnMetadata("col5"),
                new ColumnMetadata("col0"));
        Assert.assertEquals(SUT.applyToColumns(input), expected);
    }
    
    @Test(expectedExceptions = IncompatibleSliceException.class)
    public void testApplyIncompatible() throws IncompatibleSliceException {
        ColumnModel input = columns("col0", "col1", "col2", "col3", "col4");
        SUT.applyToColumns(input);
    }
    
    @Test
    public void testSerialization() {
        TestUtils.isSerializedTo(SUT, json, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testDeserialization() throws JsonParseException, JsonMappingException, IOException {
        DagSlice slice = ParsingUtilities.mapper.readValue(json, DagSlice.class);
        Assert.assertEquals(slice, SUT);
    }
    
    @Test
    public void testDeserializationPartial() throws JsonParseException, JsonMappingException, IOException {
        DagSlice slice = ParsingUtilities.mapper.readValue(jsonReorder, DagSlice.class);
        Assert.assertEquals(slice, new ReorderSlice(
                Arrays.asList("col3", "col2", "col5", "col0"),
                Collections.emptyMap()
                ));
    }
    
    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() {
        Assert.assertTrue(SUT.equals(new ReorderSlice(
                Arrays.asList("col3", "col2", "col5", "col0"),
                Collections.singletonMap("col2", "renamed"))));
        Assert.assertFalse(SUT.equals("foo"));
    }
    
    @Test
    public void testHashCode() {
        Assert.assertEquals(SUT.hashCode(), new ReorderSlice(
                Arrays.asList("col3", "col2", "col5", "col0"),
                Collections.singletonMap("col2", "renamed")).hashCode());
    }
    
    @Test
    public void testToString() {
        Assert.assertEquals(SUT.toString(), "[ReorderSlice with final columns \"col3\", \"renamed\", \"col5\", \"col0\"]");
    }
}
