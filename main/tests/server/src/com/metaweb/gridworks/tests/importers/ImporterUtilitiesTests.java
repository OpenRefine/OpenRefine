package com.metaweb.gridworks.tests.importers;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.metaweb.gridworks.importers.ImporterUtilities;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ImporterUtilitiesTests {
    // logging
    final static protected Logger logger = LoggerFactory.getLogger("ImporterUtilitiesTests");

    @Test(enabled=false)
    public void parseCellValueWithText(){
        String END_QUOTES_SHOULD_BE_RETAINED = "\"To be\" is almost always followed by \"or not to be\"";
        String response = (String) ImporterUtilities.parseCellValue(END_QUOTES_SHOULD_BE_RETAINED);
        Assert.assertEquals(response, END_QUOTES_SHOULD_BE_RETAINED);
    }

    @Test
    public void getIntegerOption(){
        Properties options = mock(Properties.class);
        when(options.containsKey("testInteger")).thenReturn(true);
        when(options.getProperty("testInteger")).thenReturn("5");
        int response = ImporterUtilities.getIntegerOption("testInteger", options, -1);
        Assert.assertEquals(5, response);
        verify(options, times(1)).containsKey("testInteger");
        verify(options, times(1)).getProperty("testInteger");
    }

    @Test
    public void getIntegerOptionReturnsDefaultOnError(){
        Properties options = mock(Properties.class);
        when(options.containsKey("testInteger")).thenReturn(true);
        when(options.getProperty("testInteger")).thenReturn("notAnInteger");
        int response = ImporterUtilities.getIntegerOption("testInteger", options, -1);
        Assert.assertEquals(-1, response);
        verify(options, times(1)).containsKey("testInteger");
        verify(options, times(1)).getProperty("testInteger");
    }

    @Test
    public void appendColumnName(){
        List<String> columnNames = new ArrayList<String>();


        ImporterUtilities.appendColumnName(columnNames, 0, "foo");
        ImporterUtilities.appendColumnName(columnNames, 1, "bar");
        Assert.assertEquals(columnNames.size(), 2);
        Assert.assertEquals(columnNames.get(0), "foo");
        Assert.assertEquals(columnNames.get(1), "bar");
    }

    @Test
    public void appendColumnNameFromMultipleRows(){
        List<String> columnNames = new ArrayList<String>();

        ImporterUtilities.appendColumnName(columnNames, 0, "foo");
        ImporterUtilities.appendColumnName(columnNames, 0, "bar");
        Assert.assertEquals(columnNames.size(), 1);
        Assert.assertEquals(columnNames.get(0), "foo bar");
    }

    @Test
    public void ensureColumnsInRowExist(){
        String VALUE_1 = "value1";
        String VALUE_2 = "value2";
        Row row = new Row(2);
        ArrayList<String> columnNames = new ArrayList<String>(2);
        columnNames.add(VALUE_1);
        columnNames.add(VALUE_2);

        ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

        Assert.assertEquals(columnNames.size(), 2);
        Assert.assertEquals(columnNames.get(0), VALUE_1);
        Assert.assertEquals(columnNames.get(1), VALUE_2);
    }

    @Test
    public void ensureColumnsInRowExistDoesExpand(){
        Row row = new Row(4);
        for(int i = 1; i < 5; i++)
            row.cells.add(new Cell("value" + i, null));

        ArrayList<String> columnNames = new ArrayList<String>(2);


        ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertEquals(columnNames.size(), 4);
    }

    @Test
    public void setupColumns(){
        Project project = new Project();
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("col1");
        columnNames.add("col2");
        columnNames.add("");
        ImporterUtilities.setupColumns(project, columnNames);
        Assert.assertEquals( project.columnModel.columns.get(0).getName(), "col1" );
        Assert.assertEquals( project.columnModel.columns.get(1).getName(), "col2" );
        Assert.assertEquals( project.columnModel.columns.get(2).getName(), "Column");
    }

}
