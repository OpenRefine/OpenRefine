/*******************************************************************************
 * Copyright (C) 2018, Antonin Delpeuch
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

package com.google.refine.commands.browsing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class GetClusteringFunctionsAndDistancesCommandTest {

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected StringWriter writer = null;
    protected Command command = null;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        command = new GetClusteringFunctionsAndDistancesCommand();
        writer = new StringWriter();
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetFunctionsAndKeyers() throws ServletException, IOException {
        command.doGet(request, response);
        ObjectNode result = ParsingUtilities.mapper.readValue(writer.toString(), ObjectNode.class);
        assertTrue(Arrays.asList(JSONUtilities.getStringArray(result, "keyers")).contains("metaphone3"));
        assertTrue(Arrays.asList(JSONUtilities.getStringArray(result, "distances")).contains("levenshtein"));
    }
}
