package com.google.refine.tests.importers;

import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;

import org.json.JSONObject;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImportingParserBase;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.XmlImportUtilities;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;

abstract class ImporterTest extends RefineTest {
    //mock dependencies
    protected Project project;
    protected ProjectMetadata metadata;
    protected ImportingJob job;
    
    protected JSONObject options;
    
    public void SetUp(){
        //FIXME - should we try and use mock(Project.class); - seems unnecessary complexity
        project = new Project();
        metadata = new ProjectMetadata();
        job = ImportingManager.createJob();
        
        options = mock(JSONObject.class);
    }
    
    public void TearDown(){
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
    
    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        parser.parseOneFile(
            project,
            metadata,
            job,
            "file-source",
            inputStream,
            rootColumnGroup,
            -1,
            options,
            new ArrayList<Exception>()
        );
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.columnModel.update();
    }
}
