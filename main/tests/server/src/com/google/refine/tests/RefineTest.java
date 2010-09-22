package com.google.refine.tests;

import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class RefineTest {

    protected Logger logger;

    @BeforeSuite
    public void init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
    }
            
    public static void assertProjectCreated(Project project, int numCols, int numRows) {
        Assert.assertNotNull(project);
        Assert.assertNotNull(project.columnModel);
        Assert.assertNotNull(project.columnModel.columns);
        Assert.assertEquals(project.columnModel.columns.size(), numCols);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), numRows);
    }

    public void log(Project project) {
        // some quick and dirty debugging
        StringBuilder sb = new StringBuilder();
        for(Column c : project.columnModel.columns){
            sb.append(c.getName());
            sb.append("; ");
        }
        logger.info(sb.toString());
        for(Row r : project.rows){
            sb = new StringBuilder();
            for(int i = 0; i < r.cells.size(); i++){
                Cell c = r.getCell(i);
                if(c != null){
                   sb.append(c.value);
                   sb.append("; ");
                }else{
                    sb.append("null; ");
                }
            }
            logger.info(sb.toString());
        }
    }
}
