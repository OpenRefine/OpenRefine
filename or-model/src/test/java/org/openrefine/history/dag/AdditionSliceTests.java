package org.openrefine.history.dag;

import static org.openrefine.history.dag.DagSliceTestHelper.columns;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class AdditionSliceTests {
    public static final String json = "{\n" + 
            "       \"inputs\" : [ \"col2\", \"col3\" ],\n" + 
            "       \"outputs\" : [ {\n" + 
            "         \"name\" : \"newcol\",\n" + 
            "         \"originalName\" : \"newcol\"\n" + 
            "       } ],\n" + 
            "       \"position\" : 2,\n" + 
            "       \"type\" : \"addition\"\n" + 
            "     }";
    
    private AdditionSlice SUT;
    
    @BeforeMethod
    public void setUp() {
        SUT = new AdditionSlice(
                Arrays.asList("col2", "col3").stream().collect(Collectors.toSet()),
                Arrays.asList(new ColumnMetadata("newcol")),
               2);
    }
    
    @Test
    public void testApplyToColumns() throws IncompatibleSliceException {
        ColumnModel input = columns("col1", "col2", "col3", "col4");
        ColumnModel expected = columns("col1", "col2", "newcol", "col3", "col4");
        Assert.assertEquals(SUT.applyToColumns(input), expected);
    }
    
    @Test(expectedExceptions = IncompatibleSliceException.class)
    public void testMissingDependency() throws IncompatibleSliceException {
        ColumnModel columns = columns("col12", "col22", "col32", "col42");
        SUT.applyToColumns(columns);
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
    
    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() {
        Assert.assertTrue(SUT.equals(new AdditionSlice(
                Arrays.asList("col2", "col3").stream().collect(Collectors.toSet()),
                Arrays.asList(new ColumnMetadata("newcol")),
               2)));
        Assert.assertFalse(SUT.equals("foo"));
    }
    
    @Test
    public void testHashCode() {
        Assert.assertEquals(SUT.hashCode(), new AdditionSlice(
                Arrays.asList("col2", "col3").stream().collect(Collectors.toSet()),
                Arrays.asList(new ColumnMetadata("newcol")),
               2).hashCode());
    }
    
    @Test
    public void testToString() {
        Assert.assertEquals(SUT.toString(), "[AdditionSlice for \"newcol\"]");
    }
}
