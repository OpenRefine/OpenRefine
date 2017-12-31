package com.google.refine.tests.commands.project;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.google.refine.ProjectManager;
import com.google.refine.commands.project.SetMetadataCommand;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.DataPackageMetadata;
import com.google.refine.model.medadata.MetadataFactory;
import com.google.refine.model.medadata.MetadataFormat;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetadataFactory.class)
public class SetMetadataCommandTests  {
    
    Logger logger = LoggerFactory.getLogger(SetMetadataCommandTests.class.getClass());
    
    // System Under Test
    SetMetadataCommand SUT = null;

    // variables
    long PROJECT_ID_LONG = 1234;
    String PROJECT_ID = "1234";
    private static final String LICENSE = "Apache License 2.0";
    private String changedJSON;
    private DataPackageMetadata metadata;

    // mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    ProjectManager projMan = null;
    Project proj = null;
    PrintWriter pw = null;

    @Before
    public void SetUp() throws JSONException, IOException {
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        proj = mock(Project.class);
        pw = mock(PrintWriter.class);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        SUT = new SetMetadataCommand();
        
        metadata = new DataPackageMetadata();
        String content = getJSONContent("datapackage-sample.json");
        changedJSON = content.replace("PDDL-1.0", LICENSE);
        
        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(request.getParameter("format")).thenReturn("DATAPACKAGE_METADATA");
        when(request.getParameter("jsonContent")).thenReturn(changedJSON);
        when(projMan.getProject(anyLong())).thenReturn(proj);
        
        //given
        PowerMockito.mockStatic(MetadataFactory.class);
        BDDMockito.given(MetadataFactory.buildMetadata(MetadataFormat.DATAPACKAGE_METADATA)).willReturn(metadata);
        
        try {
            when(response.getWriter()).thenReturn(pw);
        } catch (IOException e1) {
            Assert.fail();
        }
    }

    @After
    public void TearDown() {
        SUT = null;

        projMan = null;
        ProjectManager.singleton = null;
        proj = null;
        pw = null;
        request = null;
        response = null;
    }

    /**
     *  Contract for a complete working post
     * @throws IOException 
     */
    @Test
    public void setMetadataTest()  {
        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("format");      
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);

        try {
            verify(response, times(1)).getWriter();
        } catch (IOException e) {
            Assert.fail();
        }
        verify(pw, times(1)).write("{ \"code\" : \"ok\" }");
        
        Assert.assertEquals(proj.getMetadata().getJSON().get("license"), LICENSE);
    }
    
    
     @Test
     public void doPostThrowsIfCommand_getProjectReturnsNull(){
         when(request.getParameter("format")).thenReturn("DATAPACKAGE_METADATA");
        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            //expected
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(1)).getParameter("format");
        verify(request).getParameter("project");
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);
     }
     
     private String getJSONContent(String fileName) throws IOException {
         InputStream in = this.getClass().getClassLoader()
                 .getResourceAsStream(fileName);
         String content = org.apache.commons.io.IOUtils.toString(in);
         
         return content;
     }
}
