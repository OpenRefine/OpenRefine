package com.google.refine.tests.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.when;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;

import com.google.refine.RefineServlet;
import com.google.refine.importers.ImportingParserBase;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.XmlImportUtilities;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.model.metadata.ProjectMetadata;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.RefineTest;

abstract public class ImporterTest extends RefineTest {
    //mock dependencies
    protected Project project;
    protected ProjectMetadata metadata;
    protected ImportingJob job;
    protected RefineServlet servlet;
    
    protected JSONObject options;
    
    public void setUp(){
        //FIXME - should we try and use mock(Project.class); - seems unnecessary complexity

        servlet = new RefineServletStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();
        ImportingJob spiedJob = ImportingManager.createJob();
        job = Mockito.spy(spiedJob);
        when(job.getRetrievalRecord()).thenReturn(new JSONObject());
        
        options = Mockito.mock(JSONObject.class);
    }
    
    public void tearDown(){
        project = null;
        metadata = null;
        
        ImportingManager.disposeJob(job.id);
        job = null;
        
        options = null;
    }
    
    protected void parseOneFile(ImportingParserBase parser, Reader reader) {
        parser.parseOneFile(
            project,
            metadata,
            job,
            "file-source",
            reader,
            -1,
            options,
            new ArrayList<Exception>()
        );
        project.update();
    }
    
    protected void parseOneFile(ImportingParserBase parser, InputStream inputStream) {
        parser.parseOneFile(
            project,
            metadata,
            job,
            "file-source",
            inputStream,
            -1,
            options,
            new ArrayList<Exception>()
        );
        project.update();
    }
    
    protected void parseOneFile(TreeImportingParserBase parser, Reader reader) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        parser.parseOneFile(
            project,
            metadata,
            job,
            "file-source",
            reader,
            rootColumnGroup,
            -1,
            options,
            new ArrayList<Exception>()
        );
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.columnModel.update();
    }
    
    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream, JSONObject options) {
        parseOneInputStreamAsReader(parser, inputStream, options);
    }
    
    protected void parseOneInputStream(
            TreeImportingParserBase parser, InputStream inputStream, JSONObject options) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<Exception>();
        
        parser.parseOneFile(
            project,
            metadata,
            job,
            "file-source",
            inputStream,
            rootColumnGroup,
            -1,
            options,
            exceptions
        );
        postProcessProject(project, rootColumnGroup, exceptions);
    }

    protected void parseOneInputStreamAsReader(
            TreeImportingParserBase parser, InputStream inputStream, JSONObject options) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<Exception>();
        
        Reader reader = new InputStreamReader(inputStream);
        parser.parseOneFile(
            project,
            metadata,
            job,
            "file-source",
            reader,
            rootColumnGroup,
            -1,
            options,
            exceptions
        );
        postProcessProject(project, rootColumnGroup, exceptions);
        
        try {
            reader.close();
        } catch (IOException e) {
            //ignore errors on close
        }
    }
    
    protected void postProcessProject(
        Project project, ImportColumnGroup rootColumnGroup, List<Exception> exceptions) {
        
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.update();
        
        for (Exception e : exceptions) {
            e.printStackTrace();
        }
    }
}
