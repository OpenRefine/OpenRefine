package org.openrefine.history.dag;

import java.io.IOException;
import java.util.Arrays;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class OpaqueSliceTests {
    
    public static final String json = "{\n" + 
            "       \"columnModel\" : {\n" + 
            "         \"columns\" : [ {\n" + 
            "           \"name\" : \"foo\",\n" + 
            "           \"originalName\" : \"foo\"\n" + 
            "         }, {\n" + 
            "           \"name\" : \"bar\",\n" + 
            "           \"originalName\" : \"bar\"\n" + 
            "         } ],\n" + 
            "         \"keyCellIndex\" : 0,\n" + 
            "         \"keyColumnName\" : \"foo\"\n" + 
            "       },\n" + 
            "       \"type\" : \"opaque\"\n" + 
            "     }";
    
    private OpaqueSlice SUT;
    
    @BeforeMethod
    public void setUp() {
        SUT = new OpaqueSlice(
                new ColumnModel(Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"))));
    }
    
    @Test
    public void testApply() throws IncompatibleSliceException {
        Assert.assertEquals(SUT.applyToColumns(mock(ColumnModel.class)), SUT.getColumnModel());
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
        Assert.assertTrue(SUT.equals(new OpaqueSlice(
                new ColumnModel(
                        Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"))))));
        Assert.assertFalse(SUT.equals("foo"));
    }
    
    @Test
    public void testHashCode() {
        Assert.assertEquals(SUT.hashCode(), new OpaqueSlice(
                new ColumnModel(
                        Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar")))).hashCode()); 
    }
    
    @Test
    public void testToString() {
        Assert.assertEquals(SUT.toString(), "[OpaqueSlice]");
    }
}
