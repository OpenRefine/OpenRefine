package com.google.refine.tests.commands.project;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.Assert;

import com.google.refine.ProjectManager;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.model.Column;
import com.google.refine.model.metadata.validator.ValidateOperation;
import com.google.refine.model.metadata.DataPackageMetadata;
import com.google.refine.model.metadata.MetadataFactory;
import com.google.refine.model.metadata.MetadataFormat;
import com.google.refine.tests.importers.TsvCsvImporterTests;
import com.google.refine.util.ParsingUtilities;

import io.frictionlessdata.tableschema.Field;
import io.frictionlessdata.tableschema.exceptions.ForeignKeyException;
import io.frictionlessdata.tableschema.exceptions.PrimaryKeyException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetadataFactory.class)
public class ValidateOperationTests extends TsvCsvImporterTests  {
    private SeparatorBasedImporter parser = null;

    // variables
    private static String input;
    private DataPackageMetadata dataPackageMetadata;

    // mocks
    private ProjectManager projMan = null;
    
    private String optionsString;
    
    @BeforeClass
    public static void readData() throws IOException{
        //create input to test with
        input =  getFileContent("gdp.csv");
        // create an data type issue on the fly
         input = input.replace("28434203615.4795", "XXXXXXXXXXXXX");
//        String input = "Country Name,Country Code,Year,Value\n" + 
//                "Arab World,ARB,1968,25760683041.0826\n" + 
//                "China, CHN,1968,16289212000\n" + 
//                "Arab World,ARB,1969,28434203615.4795XXX\n";
    }
    
    @Before
    public void SetUp() throws JSONException, IOException, ValidationException, PrimaryKeyException, ForeignKeyException {
        super.setUp();
        
        optionsString = "{\"columnNames\": [\"Country Name\",\"Country Code\",\"Year\",\"Value\"]}";
        
        // mockup
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        
        dataPackageMetadata = new DataPackageMetadata();
        String content = getJSONContent("datapackage-sample.json");
        dataPackageMetadata.loadFromStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8.name())));
        
        // mock dependencies
        //given
        PowerMockito.mockStatic(MetadataFactory.class);
        BDDMockito.given(MetadataFactory.buildMetadata(MetadataFormat.DATAPACKAGE_METADATA)).willReturn(dataPackageMetadata);
        
        parser = new SeparatorBasedImporter();
        parseOneFile();
    }

    @After
    public void TearDown() {
        projMan = null;
        ProjectManager.singleton = null;
        project = null;
    }
    
    /**
     *  type or format error
     */
    @Test
    public void testTypeorFormatError()  {
        JSONObject optionObj = new JSONObject(optionsString);
        // run
        JSONObject result = startValidateOperation(optionObj);
        
        Assert.assertTrue(result.getJSONArray("validation-reports").length() > 0,
                "should get records in report");
    }
    
    @Test
    public void testMinimumConstraint() {
        // options
        optionsString = "{\"columnNames\": [\"Year\"]}";
        JSONObject optionObj = new JSONObject(optionsString);
        
        // add Constraint
        String contraintKey = Field.CONSTRAINT_KEY_MINIMUM;
        String contraintValue = "1962";
        Column column = project.columnModel.getColumnByName("Year");
        column.setType(Field.FIELD_TYPE_STRING);
        addConstraint(column, contraintKey,  contraintValue);
        
        // run
        JSONObject result = startValidateOperation(optionObj);
        
        Assert.assertTrue(result.getJSONArray("validation-reports").length() > 0,
                "should get records in report");
    }
    
    @Test
    public void testMaximumConstraint() {
        // options
        optionsString = "{\"columnNames\": [\"Year\"]}";
        JSONObject optionObj = new JSONObject(optionsString);
        
        // add Constraint
        String contraintKey = Field.CONSTRAINT_KEY_MAXIMUM;
        String contraintValue = "2015";
        Column column = project.columnModel.getColumnByName("Year");
        column.setType(Field.FIELD_TYPE_STRING);
        addConstraint(column, contraintKey,  contraintValue);
        
        // run
        JSONObject result = startValidateOperation(optionObj);
        
        Assert.assertTrue(result.getJSONArray("validation-reports").length() > 0,
                "should get records in report");
    }
    
    @Test
    public void testPatternConstraint() {
        // options
        optionsString = "{\"columnNames\": [\"Year\"]}";
        JSONObject optionObj = new JSONObject(optionsString);
        
        // add Constraint
        String contraintKey = Field.CONSTRAINT_KEY_PATTERN;
        String contraintValue = "[0-9]{4}";
        Column column = project.columnModel.getColumnByName("Year");
        column.setType(Field.FIELD_TYPE_STRING);
        addConstraint(column, contraintKey,  contraintValue);
        
        // run
        JSONObject result = startValidateOperation(optionObj);
        
        Assert.assertTrue(result.getJSONArray("validation-reports").length() == 0,
                "should NOT get records in re"
                + "port");
    }
    
    @Test
    public void testEnumerableConstraint() {
     // options
        optionsString = "{\"columnNames\": [\"Year\"]}";
        JSONObject optionObj = new JSONObject(optionsString);
        
        // add Constraint
        String contraintKey = Field.CONSTRAINT_KEY_ENUM;
        String contraintValueStr = "[\"2010\",\"1990\",\"2015\"]";
        List<Object> contraintValue = ParsingUtilities.evaluateJsonStringToArray(contraintValueStr).toList();
        Column column = project.columnModel.getColumnByName("Year");
        column.setType(Field.FIELD_TYPE_STRING);
        addConstraint(column, contraintKey,  contraintValue);
        
        // run
        JSONObject result = startValidateOperation(optionObj);
        
        Assert.assertTrue(result.getJSONArray("validation-reports").length() > 0,
                "should get records in report");
    }
    
    @Test
    public void testEnumerableConstraint_ShouldReturnEmpty() {
     // options
        optionsString = "{\"columnNames\": [\"Year\"]}";
        JSONObject optionObj = new JSONObject(optionsString);
        
        // add Constraint
        String contraintKey = Field.CONSTRAINT_KEY_ENUM;
        String contraintValueStr = "[";
        for (int i = 1950;i < 2018;i++) {
            contraintValueStr += "\"" + i + "\",";
        }
        contraintValueStr += "]";
        
        List<Object> contraintValue = ParsingUtilities.evaluateJsonStringToArray(contraintValueStr).toList();
        Column column = project.columnModel.getColumnByName("Year");
        column.setType(Field.FIELD_TYPE_STRING);
        addConstraint(column, contraintKey,  contraintValue);
        
        // run
        JSONObject result = startValidateOperation(optionObj);
        
        Assert.assertEquals(result.getJSONArray("validation-reports").length(), 0);
    }
    
    
    private void addConstraint(Column column, String contraintKey, Object contraintValue) {
        java.lang.reflect.Field f1;
        try {
            f1 = column.getClass().getDeclaredField("constraints");
            f1.setAccessible(true);
            Map<String, Object> existingMap = (HashMap<String, Object>)f1.get(column);
            if (existingMap == null) {
                existingMap = new HashMap<String, Object>();
            } 
            existingMap.put(contraintKey, contraintValue);
            f1.set(column, existingMap);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
     
    
    private JSONObject startValidateOperation(JSONObject options) {
        // SUT
        JSONObject report = new ValidateOperation(project, options).startProcess();
        
        System.out.println("validation report:" + report.toString(2));
        
        return report;
    }
    
     private String getJSONContent(String fileName) throws IOException {
         InputStream in = this.getClass().getClassLoader()
                 .getResourceAsStream(fileName);
         String content = org.apache.commons.io.IOUtils.toString(in);
         
         return content;
     }
     

    private void parseOneFile() throws IOException{    
        String sep = ",";
         prepareOptions(sep, -1, 0, 0, 1, false, false);
         parseOneFile(parser, new StringReader(input));

         Assert.assertEquals(project.columnModel.columns.size(), 4);
     }

    private static String getFileContent(String fileName) throws IOException {
         InputStream in = ValidateOperationTests.class.getClassLoader()
                 .getResourceAsStream(fileName);
         String content = org.apache.commons.io.IOUtils.toString(in);
         
         return content;
     }
}
