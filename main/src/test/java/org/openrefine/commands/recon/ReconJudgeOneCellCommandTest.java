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
package org.openrefine.commands.recon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.commands.Command;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ReconJudgeOneCellCommandTest extends RefineTest {
        
    Project project = null;
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;
    PrintWriter writer = null;
    
    @BeforeMethod
    public void setUp() {
        GridState grid = createGrid(
                new String[] {"reconciled column","unreconciled column"},
                new Serializable[][] {
                	{"a","b"},
                	{"c","d"}});
        ReconConfig config = new StandardReconConfig(
                "http://my.recon.service/api",
                "http://my.recon.service/rdf/space",
                "http://my.recon.service/rdf/schema",
                "type3894", 
                "octopus",
                true,
                Collections.emptyList(),
                5);
        grid = grid.withColumnModel(grid.getColumnModel().withReconConfig(0, config));
        project = new Project(grid, mock(ChangeDataStore.class));
        ProjectMetadata meta = new ProjectMetadata();
    	meta.setName("test project");
        ProjectManager.singleton.registerProject(project, meta);
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        
        when(request.getParameter("project")).thenReturn(String.valueOf(project.getId()));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        
        writer = mock(PrintWriter.class);
        try {
            when(response.getWriter()).thenReturn(writer);
        } catch (IOException e1) {
            Assert.fail();
        }
        
        command = new  ReconJudgeOneCellCommand();
    }
    
    @AfterMethod
    public void tearDown() {
        ProjectManager.singleton.deleteProject(project.getId());
    }
    
    @Test
    public void testMarkOneCellInReconciledColumn() throws Exception {

        when(request.getParameter("row")).thenReturn("0");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("judgment")).thenReturn("new");
        command.doPost(request, response);
        
        Cell cell = project.getCurrentGridState().getRow(0L).cells.get(0);
        Assert.assertEquals(Recon.Judgment.New, cell.recon.judgment);
        Assert.assertEquals("http://my.recon.service/rdf/space", cell.recon.identifierSpace);
    }
    
    @Test
    public void testMarkOneCellWithCustomSpace() throws Exception {

        when(request.getParameter("row")).thenReturn("0");
        when(request.getParameter("cell")).thenReturn("0");
        when(request.getParameter("judgment")).thenReturn("new");
        when(request.getParameter("identifierSpace")).thenReturn("http://my.custom.space/id");
        when(request.getParameter("schemaSpace")).thenReturn("http://my.custom.space/schema");
        command.doPost(request, response);
        
        Cell cell = project.getCurrentGridState().getRow(0L).cells.get(0);
        Assert.assertEquals(Recon.Judgment.New, cell.recon.judgment);
        Assert.assertEquals("http://my.custom.space/id", cell.recon.identifierSpace);
        Assert.assertEquals("http://my.custom.space/schema", cell.recon.schemaSpace);
    }
}
