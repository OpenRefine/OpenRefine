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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.mockito.Mockito;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingJob.RetrievalRecord;
import org.openrefine.importing.ImportingParser;
import org.openrefine.model.GridState;
import org.openrefine.util.TestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteStreams;

public abstract class ImporterTest extends RefineTest {
    //mock dependencies
    protected ProjectMetadata metadata;
    protected ImportingJob job;
    
    protected ObjectNode options;
    protected File importerTestDir;
    
    @BeforeSuite
    public void setUpImporterTestDirectory() throws IOException {
        importerTestDir = TestUtils.createTempDirectory("openrefine-test-importer-dir");
    }
    
    @BeforeMethod
    public void setUp(){
        metadata = new ProjectMetadata();
        job = mock(ImportingJob.class);
        when(job.getRetrievalRecord()).thenReturn(new RetrievalRecord());
        when(job.getRawDataDir()).thenReturn(importerTestDir);
        
        options = Mockito.mock(ObjectNode.class);
    }
    
    @AfterMethod
    public void tearDown(){
        metadata = null;
        job = null;
        options = null;
    }

    protected GridState parseOneFile(ReaderImporter parser, Reader reader) throws Exception {
        return parseOneFile(parser, reader, options);
    }
    
    protected GridState parseOneFile(ReaderImporter parser, Reader reader, ObjectNode options) throws Exception {
        return parser.parseOneFile(
            metadata,
            job,
            "file-source",
            reader,
            -1,
            options
        );
    }
    
    protected GridState parseOneFile(InputStreamImporter parser, InputStream inputStream) throws Exception {
        return parser.parseOneFile(
            metadata,
            job,
            "file-source",
            inputStream,
            -1,
            options
        );
    }
    
    protected GridState parseOneFile(InputStreamImporter parser, InputStream inputStream, ObjectNode options) throws Exception {
        return parser.parseOneFile(
            metadata,
            job,
            "file-source",
            inputStream,
            -1,
            options
        );
    }
    
    protected GridState parseOneFile(HDFSImporter parser, String sparkURI) throws Exception {
        return parser.parseOneFile(
            metadata,
            job,
            "file-source",
            sparkURI,
            -1,
            options
        );
    }
    
    protected GridState parseOneFile(ImportingParser parser, File file, ObjectNode options) throws Exception {
        ImportingFileRecord importingRecord = mock(ImportingFileRecord.class);
        when(importingRecord.getDerivedSparkURI(Mockito.any())).thenReturn(file.getAbsolutePath());
        when(importingRecord.getFile(Mockito.any())).thenReturn(file);
        return parseFiles(parser, Collections.singletonList(importingRecord), options);
    }
    
    protected GridState parseFiles(ImportingParser parser, List<ImportingFileRecord> files, ObjectNode options) throws Exception {
        return parser.parse(metadata, job, files, "format", -1, options);
    }
    
    protected GridState parseOneString(ImportingParser parser, String contents, ObjectNode options) throws Exception {
        if (parser instanceof ReaderImporter) {
            StringReader reader = new StringReader(contents);
            return parseOneFile((ReaderImporter)parser, reader, options);
        }
        File tempFile = new File(importerTestDir, Long.toString((new Random()).nextLong(), 16).replace("-", ""));
        try {
            Writer writer = new FileWriter(tempFile);
            try {
                writer.write(contents);
            } finally {
                writer.close();
            }
            return parseOneFile(parser, tempFile, options);
        } finally {
            tempFile.delete();
        }
    }
    
    protected GridState parseOneString(ImportingParser parser, String string) throws Exception {
        return parseOneString(parser, string, options);
    }
    
    /**
     * Saves an input stream to a file in the import test directory.
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    protected File saveInputStreamToImporterTestDir(InputStream inputStream) throws IOException {
        try {
            File tempFile = new File(importerTestDir, Long.toString((new Random()).nextLong(), 16).replace("-", ""));
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            ByteStreams.copy(inputStream, outputStream);
            return tempFile;
        } finally {
            inputStream.close();
        }
    }
    
    /*
    
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
