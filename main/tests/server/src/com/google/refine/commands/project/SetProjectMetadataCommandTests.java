/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.commands.project;

import static com.google.refine.util.TestUtils.assertEqualsAsJson;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class SetProjectMetadataCommandTests extends CommandTestBase {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    SetProjectMetadataCommand SUT = null;

    // variables
    long PROJECT_ID_LONG = 1234;
    String PROJECT_ID = Long.toString(PROJECT_ID_LONG);
    String SUBJECT = "subject for project";

    // mocks
    ProjectManager projMan = null;
    Project proj = null;

    @BeforeMethod
    public void SetUp() throws IOException {
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        proj = mock(Project.class);
        writer = new StringWriter();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        SUT = new SetProjectMetadataCommand();

        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setUserMetadata((ArrayNode) ParsingUtilities.mapper.readTree("[ {name: \"clientID\", display: true} ]"));

        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(projMan.getProject(anyLong())).thenReturn(proj);
        when(proj.getMetadata()).thenReturn(metadata);

        when(response.getWriter()).thenReturn(new PrintWriter(writer));
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;

        projMan = null;
        ProjectManager.singleton = null;
        proj = null;
        writer = null;
        request = null;
        response = null;
    }

    /**
     * Contract for a complete working post
     */
    @Test
    public void setMetadataTest() throws Exception {
        when(request.getParameter("name")).thenReturn("subject");
        when(request.getParameter("value")).thenReturn(SUBJECT);

        // run
        SUT.doPost(request, response);

        // verify
        verify(request, times(2)).getParameter("project");
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);

        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        verify(proj, times(1)).getMetadata();
        verify(response, times(1)).getWriter();
        assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");

        Assert.assertEquals(proj.getMetadata().getSubject(), SUBJECT);
    }

    /**
     * set a user defined metadata field
     */
    @Test
    public void setUserMetadataFieldTest() throws Exception {
        when(request.getParameter("name")).thenReturn("clientID");
        when(request.getParameter("value")).thenReturn("IBM");

        // run
        SUT.doPost(request, response);

        // verify
        verify(request, times(2)).getParameter("project");
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);

        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        verify(proj, times(1)).getMetadata();
        verify(response, times(1)).getWriter();
        assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");

        ObjectNode obj = (ObjectNode) proj.getMetadata().getUserMetadata().get(0);
        Assert.assertEquals(obj.get("name").asText(), "clientID");
        Assert.assertEquals(obj.get("value").asText(), "IBM");
    }

    @Test
    public void doPostThrowsIfCommand_getProjectReturnsNull() throws Exception {
        // run
        SUT.doPost(request, response);

        // verify
        verify(request, times(2)).getParameter("project");
        verify(projMan, times(1)).getProject(PROJECT_ID_LONG);
    }

    @Test
    public void setCustomMetadataTest() {
        String customMetadata = this.getValidCustomMetadata();
        ProjectMetadata meta = proj.getMetadata();
        meta.setAnyField("customMetadata", customMetadata);

        Assert.assertEquals(meta.getCustomMetadata("name"), "test");
        Assert.assertEquals(meta.getCustomMetadata("isValid"), true);
        Assert.assertEquals(meta.getCustomMetadata("id"), 1);
        Assert.assertTrue(meta.getCustomMetadata("companies") instanceof List);
        Assert.assertTrue(meta.getCustomMetadata("information") instanceof Map);
    }

    @Test
    public void setInvalidCustomMetadataTest() {
        String customMetadata = this.getInvalidCustomMetadata();
        ProjectMetadata meta = proj.getMetadata();

        Assert.assertThrows(() -> meta.setAnyField("customMetadata", customMetadata));
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn("");
        SUT.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testNoProjectId() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(null);
        SUT.doPost(request, response);
        assertErrorNotCSRF();
    }

    private String getValidCustomMetadata() {
        return "{\n" + //
                "  \"name\":\"test\",\n" + //
                "  \"companies\":[\"Google\", \"IBM\", \"meta\"],\n" + //
                "  \"isValid\": true,\n" + //
                "  \"information\": {\n" + //
                "    \"text-field\":\"value\",\n" + //
                "    \"numeric-field\": 1\n" + //
                "  },\n" + //
                "  \"id\":1\n" + //
                "}";
    }

    private String getInvalidCustomMetadata() {
        return "{\n  \"name\":test\n}";
    }
}
