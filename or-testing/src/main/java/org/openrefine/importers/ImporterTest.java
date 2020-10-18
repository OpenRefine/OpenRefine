/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.importers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.Reader;

import org.mockito.Mockito;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.importers.ImporterUtilities.MultiFileReadingProgress;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingJob.RetrievalRecord;
import org.openrefine.model.GridState;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class ImporterTest extends RefineTest {
    //mock dependencies
    protected ProjectMetadata metadata;
    protected ImportingJob job;
    
    protected ObjectNode options;
    
    public void setUp(){
        metadata = new ProjectMetadata();
        job = mock(ImportingJob.class);
        when(job.getRetrievalRecord()).thenReturn(new RetrievalRecord());
        
        options = Mockito.mock(ObjectNode.class);
    }
    
    public void tearDown(){
        metadata = null;
        job = null;
        options = null;
    }

    protected GridState parseOneFile(ImportingParserBase parser, Reader reader) throws Exception {
        return ((ReaderImporter)parser).parseOneFile(
            metadata,
            job,
            "file-source",
            reader,
            -1,
            options
        );
    }
    
    protected GridState parseOneFile(ImportingParserBase parser, InputStream inputStream) throws Exception {
        return ((InputStreamImporter)parser).parseOneFile(
            metadata,
            job,
            "file-source",
            inputStream,
            -1,
            options
        );
    }
    
    protected GridState parseOneFile(ImportingParserBase parser, String sparkURI) throws Exception {
        return ((HDFSImporter)parser).parseOneFile(
            metadata,
            job,
            "file-source",
            sparkURI,
            -1,
            options
        );
    }
    
    /*
    protected void parseOneFile(TreeImportingParserBase parser, Reader reader) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        parser.parseOneFile(
            metadata,
            job,
            "file-source",
            reader,
            rootColumnGroup,
            -1,
            options
        );
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
    }
    
    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
        parseOneInputStreamAsReader(parser, inputStream, options);
    }
    
    protected void parseOneInputStream(
            TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<Exception>();
        
        parser.parseOneFile(
            metadata,
            job,
            "file-source",
            inputStream,
            rootColumnGroup,
            -1,
            options
        );
        postProcessProject(project, rootColumnGroup, exceptions);
    }

    protected void parseOneInputStreamAsReader(
            TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
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
        
        for (Exception e : exceptions) {
            e.printStackTrace();
        }
    }
    */
}
