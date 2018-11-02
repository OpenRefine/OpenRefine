package com.google.refine.tests.operations.cell;

import java.util.List;

import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.MassEditOperation;
import com.google.refine.operations.cell.MassEditOperation.Edit;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class MassOperationTests extends RefineTest {

    private List<Edit> editList;
    private String editsString;
    
    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "mass-edit", MassEditOperation.class);
    }
    
    @Test
    public void serializeMassEditOperation() throws JSONException, Exception {
        String json = "{\"op\":\"core/mass-edit\","
                + "\"description\":\"Mass edit cells in column my column\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\",\"expression\":\"value\","
                + "\"edits\":[{\"fromBlank\":false,\"fromError\":false,\"from\":[\"String\"],\"to\":\"newString\"}]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MassEditOperation.class), json);
    }

    @Test
    public void testReconstructEditString() throws Exception {
        editsString = "[{\"from\":[\"String\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(ParsingUtilities.evaluateJsonStringToArray(editsString).toString(), new TypeReference<List<Edit>>() {});

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "String");
        Assert.assertEquals(editList.get(0).to,"newString" );
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditMultiString() throws Exception {
        editsString = "[{\"from\":[\"String1\",\"String2\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(ParsingUtilities.evaluateJsonStringToArray(editsString).toString(), new TypeReference<List<Edit>>() {});

        Assert.assertEquals(editList.get(0).from.size(), 2);
        Assert.assertEquals(editList.get(0).from.get(0), "String1");
        Assert.assertEquals(editList.get(0).from.get(1), "String2");
        Assert.assertEquals(editList.get(0).to,"newString" );
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditBoolean() throws Exception {
      editsString = "[{\"from\":[true],\"to\":\"newString\",\"type\":\"text\"}]";

      editList = ParsingUtilities.mapper.readValue(ParsingUtilities.evaluateJsonStringToArray(editsString).toString(), new TypeReference<List<Edit>>() {});

      Assert.assertEquals(editList.get(0).from.size(), 1);
      Assert.assertEquals(editList.get(0).from.get(0), "true");
      Assert.assertEquals(editList.get(0).to,"newString" );
      Assert.assertFalse(editList.get(0).fromBlank);
      Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditNumber() throws Exception {
      editsString = "[{\"from\":[1],\"to\":\"newString\",\"type\":\"text\"}]";

      editList = ParsingUtilities.mapper.readValue(ParsingUtilities.evaluateJsonStringToArray(editsString).toString(), new TypeReference<List<Edit>>() {});

      Assert.assertEquals(editList.get(0).from.size(), 1);
      Assert.assertEquals(editList.get(0).from.get(0), "1");
      Assert.assertEquals(editList.get(0).to,"newString" );
      Assert.assertFalse(editList.get(0).fromBlank);
      Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditDate() throws Exception {
      editsString = "[{\"from\":[\"2018-10-04T00:00:00Z\"],\"to\":\"newString\",\"type\":\"text\"}]";

      editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {});

      Assert.assertEquals(editList.get(0).from.get(0), "2018-10-04T00:00:00Z");
      Assert.assertEquals(editList.get(0).to,"newString" );
      Assert.assertFalse(editList.get(0).fromBlank);
      Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditEmpty() throws Exception {
      editsString = "[{\"from\":[\"\"],\"to\":\"newString\",\"type\":\"text\"}]";

      editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {});

      Assert.assertEquals(editList.get(0).from.size(), 1);
      Assert.assertEquals(editList.get(0).from.get(0), "");
      Assert.assertEquals(editList.get(0).to,"newString" );
      Assert.assertTrue(editList.get(0).fromBlank);
      Assert.assertFalse(editList.get(0).fromError);

    }

    //Not yet testing for mass edit from OR Error

}
