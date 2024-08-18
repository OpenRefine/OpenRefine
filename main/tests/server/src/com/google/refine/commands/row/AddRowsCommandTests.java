/*******************************************************************************
 * Copyright (C) 2024, OpenRefine contributors
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

package com.google.refine.commands.row;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class AddRowsCommandTests extends CommandTestBase {

    private Project project = null;
    private final String[] additionalRows = { "{}", "{}" };

    protected AddRowsCommand command;

    @BeforeMethod
    public void setUpCommand() {
        command = new AddRowsCommand();
        project = createProject(
                new String[] { "Column 1", "Column 2" },
                new Serializable[][] {
                        { "c", "3" },
                        { "d", "4" },
                });
        when(request.getParameter("project")).thenReturn(Long.toString(project.id));
    }

    @Test
    // If CSRF token is missing, respond with CSRF error
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    // If successful request, responses contains
    public void testSuccessResponseSchema() throws ServletException, IOException {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("0");
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);

        command.doPost(request, response);

        JsonNode node = ParsingUtilities.mapper.readValue(writer.toString(), JsonNode.class);
        assertNotNull(node.get("code"));
        assertEquals(node.get("code").toString(), "\"ok\"");
        assertNotNull(node.get("historyEntry"));
        assertNotNull(node.get("historyEntry").get("id"));
        assertNotNull(node.get("historyEntry").get("description"));
        assertNotNull(node.get("historyEntry").get("time"));
    }

    @Test(expectedExceptions = NumberFormatException.class)
    // If missing index parameter, `getInsertionIndex` throws NumberFormatException
    public void testMissingIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);

        assert !request.getParameterMap().containsKey(AddRowsCommand.INDEX_PARAMETER);

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    // If index parameter is null, `getInsertionIndex` throws NumberFormatException
    public void testNullIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn(null);

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    // If index parameter is empty string, `getInsertionIndex` throws NumberFormatException
    public void testEmptyIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("");

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    // If index parameter is a string, `getInsertionIndex` throws NumberFormatException
    public void testNonIntegerIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("A");

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    // If index parameter is a fraction, `getInsertionIndex` throws NumberFormatException
    public void testFractionIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("1.5");

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    // If index parameter is negative, `doPost` call triggers IndexOutOfBoundsException
    public void testNegativeIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("-1");

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    // If index parameter larger than project's maximum index, `getInsertionIndex` throws IllegalArgumentException
    public void testTooLargeIndexParameter() {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(additionalRows);
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn(String.valueOf(project.rows.size() + 1));

        command.getInsertionIndex(request, project);
    }

    @Test(expectedExceptions = NullPointerException.class)
    // If rows parameter is missing, `getRowData` throws NullPointerException
    public void testMissingRowsParameter() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("0");

        assert !request.getParameterMap().containsKey(AddRowsCommand.ROWS_PARAMETER);

        command.getRowData(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    // If rows parameter is empty, `getRowData` throws IllegalArgumentException
    public void testEmptyRowsParameter() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("0");
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER)).thenReturn(new String[] {});

        command.getRowData(request);
    }

    @Test(expectedExceptions = JsonParseException.class)
    // If rows parameter contains malformed JSON, `getRowData` throws JsonParseException
    public void testMalformedJSONRows() throws Exception {
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("0");
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER))
                .thenReturn(new String[] { "{ \"c\", \"3\" " }); // Intentionally missing closing brace

        command.getRowData(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    // If row parameter contains non-empty rows throw IllegalArgumentException
    public void testNonEmptyRows() throws Exception {
        String[] rows = {
                "{ starred: false, flagged: false, cells: [{v: 1}, {v:2}] }"
        };
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
        when(request.getParameter(AddRowsCommand.INDEX_PARAMETER)).thenReturn("0");
        when(request.getParameterValues(AddRowsCommand.ROWS_PARAMETER))
                .thenReturn(rows);

        command.getRowData(request);
    }
}
