package org.openrefine.history.dag;

import static org.openrefine.history.dag.DagSliceTestHelper.columns;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.openrefine.model.ColumnModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class TransformationSliceTests {
    public static final String json = "{\n" + 
            "       \"column\" : \"transformedColumn\",\n" + 
            "       \"inputs\" : [ \"transformedColumn\", \"col2\", \"col3\" ],\n" + 
            "       \"type\" : \"transformation\"\n" + 
            "     }";
    
    private TransformationSlice SUT;
    
    @BeforeMethod
    public void setUp() {
        SUT = new TransformationSlice(
                "transformedColumn",
                Arrays.asList("transformedColumn", "col2", "col3").stream().collect(Collectors.toSet()));
    }
    
    @Test
    public void testApplyColumns() throws IncompatibleSliceException {
        ColumnModel input = columns("col1", "transformedColumn", "col2", "col3");
        Assert.assertEquals(SUT.applyToColumns(input), input);
    }
    
    @Test(expectedExceptions = IncompatibleSliceException.class)
    public void testNoSuchColumn() throws IncompatibleSliceException {
        SUT.applyToColumns(columns("foo", "bar"));
    }
    
    @Test(expectedExceptions = IncompatibleSliceException.class)
    public void testMissingDependency() throws IncompatibleSliceException {
        SUT.applyToColumns(columns("transformedColumn"));
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
        Assert.assertTrue(SUT.equals(new TransformationSlice(
                "transformedColumn",
                Arrays.asList("transformedColumn", "col2", "col3").stream().collect(Collectors.toSet()))));
        Assert.assertFalse(SUT.equals("foo"));
    }
    
    @Test
    public void testToString() {
        Assert.assertEquals(SUT.toString(), "[TransformationSlice on \"transformedColumn\"]");
    }
    
    @Test
    public void testHashCode() {
        Assert.assertEquals(SUT.hashCode(),
                new TransformationSlice(
                        "transformedColumn",
                        Arrays.asList("transformedColumn", "col2", "col3").stream().collect(Collectors.toSet())).hashCode());
    }
}
