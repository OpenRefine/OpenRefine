
package com.google.refine.tests.expr.functions;

import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.HasFieldsListImpl;
import com.google.refine.expr.WrappedRow;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.ProjectManagerStub;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.RefineTest;

/**
 * Test cases for cross function.
 */
public class CrossFunctionTests extends RefineTest {
    static Properties bindings;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    RefineServlet servlet;
    Project projectGift;
    Project projectAddress;
    ProjectMetadata metadata;
    ImportingJob job;
    JSONObject options;
    SeparatorBasedImporter importer;

    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
        
        servlet = new RefineServletStub();
        ProjectManager.singleton = new ProjectManagerStub();
        ImportingManager.initialize(servlet);
        projectAddress = new Project();

        job = ImportingManager.createJob();
        options = mock(JSONObject.class);
        importer = new SeparatorBasedImporter();
        
        createMyAddressBook();
        projectGift = createChristmasGifts();
        bindings.put("project", projectGift);
        
        // add a column address based on column recipient
        bindings.put("columnName", "recipient");
    }
    
    // data from: https://github.com/OpenRefine/OpenRefine/wiki/GREL-Other-Functions
    private Project createMyAddressBook() {
        String projectName = "My Address Book";
        String input = "friend;address\n"
                        + "john;120 Main St.\n"
                        + "mary;50 Broadway Ave.\n"
                        + "john;999 XXXXXX St.\n"                       // john's 2nd address
                        + "anne;17 Morning Crescent\n";
        return createProject(projectName, input);
    }
    
    private Project createChristmasGifts() {
        String projectName = "Christmas Gifts";
        String input = "gift;recipient\n"   
                + "lamp;mary\n"
                + "clock;john\n";
        return createProject(projectName, input);
    }
    
    private Project createProject(String projectName, String input) {
        Project project = new Project();
        ProjectMetadata metadata = new ProjectMetadata();
        
        metadata.setName(projectName);
        prepareOptions(";", -1, 0, 0, 1, false, false);
        List<Exception> exceptions = new ArrayList<Exception>();
        importer.parseOneFile(project, metadata, job, "filesource", new StringReader(input), -1, options, exceptions);
        project.update();
        ProjectManager.singleton.registerProject(project, metadata);
        
        return project;
    }
    
    @AfterMethod
    public void TearDown() {
        ImportingManager.disposeJob(job.id);
        ProjectManager.singleton.deleteProject(projectGift.id);
        ProjectManager.singleton.deleteProject(projectAddress.id);
        job = null;
        metadata = null;
        projectGift = null;
        projectAddress = null;
        options = null;
        importer = null;
    }

    
    @Test
    public void crossFunctionOneToOneTest() throws Exception {
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", "mary", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "50 Broadway Ave.");
    }
    
    /**  
     * To demonstrate that the cross function can join multiple rows.
     */
    @Test
    public void crossFunctionOneToManyTest() throws Exception {
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", "john", "My Address Book", "friend")).get(1)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "999 XXXXXX St.");
    }
    

    @Test
    public void crossFunctionCaseSensitiveTest() throws Exception {
        Assert.assertNull(invoke("cross", "Anne", "My Address Book", "friend"));
    }
    
    /**
     * If no match, return null.
     * 
     * But if user still apply grel:value.cross("My Address Book", "friend")[0].cells["address"].value, 
     * from the "Preview", the target cell shows "Error: java.lang.IndexOutOfBoundsException: Index: 0, Size: 0".
     * It will still end up with blank if the onError set so.
     */
    @Test
    public void crossFunctionMatchNotFoundTest() throws Exception {
        Assert.assertNull(invoke("cross", "NON-EXIST", "My Address Book", "friend"));
    }
     
    /**
     *  
     *  rest of cells shows "Error: cross expects a string or cell, a project name to join with, and a column name in that project"
     */
    @Test
    public void crossFunctionNonLiteralValue() throws Exception {
        Assert.assertEquals(((EvalError) invoke("cross", 1, "My Address Book", "friend")).message, 
                "cross expects a string or cell, a project name to join with, and a column name in that project");
        
        Assert.assertEquals(((EvalError) invoke("cross", null, "My Address Book", "friend")).message, 
                "cross expects a string or cell, a project name to join with, and a column name in that project");
        
        Assert.assertEquals(((EvalError) invoke("cross", Calendar.getInstance(), "My Address Book", "friend")).message, 
                "cross expects a string or cell, a project name to join with, and a column name in that project");
    }
    
    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }


    private void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes) {
            
            whenGetStringOption("separator", options, sep);
            whenGetIntegerOption("limit", options, limit);
            whenGetIntegerOption("skipDataLines", options, skip);
            whenGetIntegerOption("ignoreLines", options, ignoreLines);
            whenGetIntegerOption("headerLines", options, headerLines);
            whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
            whenGetBooleanOption("processQuotes", options, !ignoreQuotes);
            whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
        }

}
