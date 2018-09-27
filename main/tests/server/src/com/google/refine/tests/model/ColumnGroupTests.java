package com.google.refine.tests.model;

import org.testng.annotations.Test;

import com.google.refine.model.ColumnGroup;
import com.google.refine.tests.util.TestUtils;

public class ColumnGroupTests {
    
    String json = "{"
            + "\"startColumnIndex\":2,"
            + "\"columnSpan\":3,"
            + "\"keyColumnIndex\":1"
            + "}";
    @Test
    public void serializeColumnGroup() throws Exception {
        TestUtils.isSerializedTo(ColumnGroup.load(json), json);
    }
    
    @Test
    public void serializeColumnGroupWithSubgroups() throws Exception {
        ColumnGroup cg = new ColumnGroup(2,3,1);
        ColumnGroup subCg = new ColumnGroup(2,2,1);
        cg.subgroups.add(subCg);
        String fullJson = "{"
                + "\"startColumnIndex\":2,"
                + "\"columnSpan\":3,"
                + "\"keyColumnIndex\":1,"
                + "\"subgroups\":[{"
                + "   \"startColumnIndex\":2,"
                + "   \"columnSpan\":2,"
                + "   \"keyColumnIndex\":1"
                + "}]"
                + "}";
        TestUtils.isSerializedTo(cg, json, true);
        TestUtils.isSerializedTo(cg, fullJson, false);
    }
}
