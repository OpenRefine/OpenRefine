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

package org.openrefine.commands.project;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;

public class SetProjectMetadataCommandTests extends CommandTestBase {

    // System Under Test
    SetProjectMetadataCommand SUT = null;

    // dependencies
    Project proj;

    // variables
    long PROJECT_ID_LONG = 1234;
    String PROJECT_ID = "1234";
    String SUBJECT = "subject for project";

    @BeforeMethod
    public void SetUp() throws Exception {

        SUT = new SetProjectMetadataCommand();

        proj = createProject(new String[] { "column 1", "column 2" }, new Serializable[][] { { "foo", "bar" }, { "test", "data" } });

        ProjectMetadata metadata = proj.getMetadata();
        metadata.setUserMetadata((ArrayNode) ParsingUtilities.mapper.readTree("[ {name: \"clientID\", display: true} ]"));
        ProjectManager.singleton.saveMetadata(metadata, proj.getId());

        // mock dependencies
        when(request.getParameter("project")).thenReturn(Long.toString(proj.getId()));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
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

        verify(response).setStatus(200);
        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        assertEquals(writer.toString(), "{\"code\":\"ok\"}");

        assertEquals(proj.getMetadata().getSubject(), SUBJECT);
    }

    /**
     * set a user defined metadata field
     */
    @Test
    public void setUserMetadataFieldTest() throws Exception {
        when(request.getParameter("name")).thenReturn("clientID");
        when(request.getParameter("value")).thenReturn("IBM");

        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(2)).getParameter("project");

        verify(response).setStatus(200);
        verify(response, times(1))
                .setHeader("Content-Type", "application/json");
        assertEquals(writer.toString(), "{\"code\":\"ok\"}");

        ObjectNode obj = (ObjectNode) proj.getMetadata().getUserMetadata().get(0);
        assertEquals(obj.get("name").asText(), "clientID");
        assertEquals(obj.get("value").asText(), "IBM");
    }

    @Test
    public void doPostThrowsIfCommand_getProjectReturnsNull() throws Exception {
        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            // expected
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        verify(request, times(2)).getParameter("project");
    }
}
