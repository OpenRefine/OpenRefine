package com.google.refine.tests.model;

import org.testng.annotations.Test;

import com.google.refine.model.ColumnModel;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ColumnModelTests extends RefineTest {
    @Test
    public void serializeColumnModel() {
        Project project = createCSVProject("a,b\n"+ 
                "e,e");
        String json = "{\n" + 
                "       \"columnGroups\" : [ ],\n" + 
                "       \"columns\" : [ {\n" + 
                "         \"cellIndex\" : 0,\n" + 
                "         \"constraints\" : \"{}\",\n" + 
                "         \"description\" : \"\",\n" + 
                "         \"format\" : \"default\",\n" + 
                "         \"name\" : \"a\",\n" + 
                "         \"originalName\" : \"a\",\n" + 
                "         \"title\" : \"\",\n" + 
                "         \"type\" : \"\"\n" + 
                "       }, {\n" + 
                "         \"cellIndex\" : 1,\n" + 
                "         \"constraints\" : \"{}\",\n" + 
                "         \"description\" : \"\",\n" + 
                "         \"format\" : \"default\",\n" + 
                "         \"name\" : \"b\",\n" + 
                "         \"originalName\" : \"b\",\n" + 
                "         \"title\" : \"\",\n" + 
                "         \"type\" : \"\"\n" + 
                "       } ],\n" + 
                "       \"keyCellIndex\" : 0,\n" + 
                "       \"keyColumnName\" : \"a\"\n" + 
                "     }";
        TestUtils.isSerializedTo(project.columnModel, json);
    }
    
    @Test
    public void serializeColumnModelEmpty() {
        String json = "{"
                + "\"columns\":[],"
                + "\"columnGroups\":[]"
                + "}";
        ColumnModel m = new ColumnModel();
        TestUtils.isSerializedTo(m, json);
    }
}
