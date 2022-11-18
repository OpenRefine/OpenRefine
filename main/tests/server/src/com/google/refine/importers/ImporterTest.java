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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
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
    protected ObjectNode fileRecord;
    protected ImporterUtilities.MultiFileReadingProgress progress = new NullProgress();

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
        when(options.deepCopy()).thenReturn(options);

        fileRecord = ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"location\": \"%s\",\"fileName\": \"%s\"}", "file:/dev/null", "file-source#worksheet"));

    }

    public void tearDown() {
        project = null;
        metadata = null;

        ImportingManager.disposeJob(job.id);
        job = null;

        options = null;
    }

    protected void parseOneFile(@NotNull ImportingParserBase parser) throws IOException {
        List<Exception> exceptions = new ArrayList<>();
        parser.parseOneFile(
                project,
                metadata,
                job,
                fileRecord,
                -1,
                options,
                exceptions,
                progress);
        assertEquals(exceptions.size(), 0);
        project.update();
    }

    protected List<Exception> parseOneFileAndReturnExceptions(@NotNull ImportingParserBase parser)
            throws IOException {
        List<Exception> exceptions = new ArrayList<>();
        parser.parseOneFile(
                project,
                metadata,
                job,
                fileRecord,
                -1,
                options,
                exceptions,
                progress);
        project.update();
        return exceptions;
    }

    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) throws IOException {
        parseOneInputStreamAsReader(parser, inputStream, options);
    }

    protected void parseOneTreeFile(
            @NotNull TreeImportingParserBase parser, ObjectNode options) throws IOException {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<>();

        parser.parseOneFile(
                project,
                metadata,
                job,
                fileRecord,
                rootColumnGroup,
                -1,
                options,
                exceptions,
                progress);
        postProcessProject(project, rootColumnGroup, exceptions);
    }

    protected void parseOneInputStreamAsReader(
            @NotNull TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) throws IOException {
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<>();

        parser.parseOneFile(
                project,
                metadata,
                job,
                fileRecord, // "file-source",
                rootColumnGroup,
                -1,
                options,
                exceptions,
                progress);
        postProcessProject(project, rootColumnGroup, exceptions);

    }

    protected void postProcessProject(
            Project project, ImportColumnGroup rootColumnGroup, @NotNull List<Exception> exceptions) {

        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.update();

        for (Exception e : exceptions) {
            e.printStackTrace();
        }
        assertEquals(exceptions.size(), 0);
    }

    protected void stageFile(File spreadsheet) throws IOException {
        FileUtils.copyFile(spreadsheet, new File(job.getRawDataDir(), spreadsheet.getName()));
        initMetadata(spreadsheet.getName());
    }

    protected void stageResource(String name) throws IOException {
        FileUtils.copyURLToFile(ClassLoader.getSystemResource(name),
                new File(job.getRawDataDir(), name));
        initMetadata(name);
    }

    protected void stageString(String contents) throws IOException {
        FileUtils.writeStringToFile(new File(job.getRawDataDir(), "foo"), contents, StandardCharsets.UTF_8);
        initMetadata("foo");
    }

    protected void stageString(String filename, String contents) throws IOException {
        FileUtils.writeStringToFile(new File(job.getRawDataDir(), filename), contents, StandardCharsets.UTF_8);
        initMetadata(filename);
    }

    private void initMetadata(String filename) {
        fileRecord = ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"location\": \"%1$s\",\"fileName\": \"%1$s\"}", filename));
    }

    private static class NullProgress implements ImporterUtilities.MultiFileReadingProgress {

        @Override
        public void startFile(String fileSource) {
        }

        @Override
        public void readingFile(String fileSource, long bytesRead) {
        }

        @Override
        public void endFile(String fileSource, long bytesRead) {
        }
    }
}
