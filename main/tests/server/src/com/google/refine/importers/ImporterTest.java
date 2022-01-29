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

package com.google.refine.importers;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.RefineServletStub;
import com.google.refine.RefineTest;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.XmlImportUtilities;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public abstract class ImporterTest extends RefineTest {

    // mock dependencies
    protected Project project;
    protected ProjectMetadata metadata;
    protected ImportingJob job;
    protected RefineServlet servlet;
    protected ObjectNode options;

    public void setUp() {
        // FIXME - should we try and use mock(Project.class); - seems unnecessary complexity

        servlet = new RefineServletStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();
        ImportingJob spiedJob = ImportingManager.createJob();
        job = Mockito.spy(spiedJob);
        when(job.getRetrievalRecord()).thenReturn(ParsingUtilities.mapper.createObjectNode());

        options = Mockito.mock(ObjectNode.class);
    }

    public void tearDown() {
        project = null;
        metadata = null;

        ImportingManager.disposeJob(job.id);
        job = null;

        options = null;
    }

    protected void parseOneFile(ImportingParserBase parser, Reader reader) {
        List<Exception> exceptions = new ArrayList<Exception>();
        parser.parseOneFile(
                project,
                metadata,
                job,
                "file-source",
                reader,
                -1,
                options,
                exceptions);
        assertEquals(exceptions.size(), 0);
        project.update();
    }

    protected void parseOneFile(ImportingParserBase parser, InputStream inputStream) {
        List<Exception> exceptions = new ArrayList<Exception>();
        parser.parseOneFile(
                project,
                metadata,
                job,
                "file-source",
                inputStream,
                -1,
                options,
                exceptions);
        assertEquals(exceptions.size(), 0);
        project.update();
    }

    protected List<Exception> parseOneFileAndReturnExceptions(ImportingParserBase parser, InputStream inputStream) {
        List<Exception> exceptions = new ArrayList<Exception>();
        parser.parseOneFile(
                project,
                metadata,
                job,
                "file-source",
                inputStream,
                -1,
                options,
                exceptions);
        project.update();
        return exceptions;
    }

    protected void parseOneFile(TreeImportingParserBase parser, Reader reader) {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<Exception>();
        parser.parseOneFile(
                project,
                metadata,
                job,
                "file-source",
                reader,
                rootColumnGroup,
                -1,
                options,
                exceptions);
        assertEquals(exceptions.size(), 0);
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.columnModel.update();
    }

    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
        parseOneInputStreamAsReader(parser, inputStream, options);
    }

    protected void parseOneInputStream(
            TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
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
                exceptions);
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
                exceptions);
        postProcessProject(project, rootColumnGroup, exceptions);

        try {
            reader.close();
        } catch (IOException e) {
            // ignore errors on close
        }
    }

    protected void postProcessProject(
            Project project, ImportColumnGroup rootColumnGroup, List<Exception> exceptions) {

        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.update();

        for (Exception e : exceptions) {
            e.printStackTrace();
        }
        assertEquals(exceptions.size(), 0);
    }
}
