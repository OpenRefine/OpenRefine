/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.commands;

import static org.mockito.Mockito.when;
import static org.openrefine.wikidata.testing.TestingData.jsonFromFile;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.Command;
import com.google.refine.util.TestUtils;

public class SaveWikibaseSchemaCommandTest extends SchemaCommandTest {

    @BeforeMethod
    public void setUp() {
        this.command = new SaveWikibaseSchemaCommand();
    }

    @Test
    public void testValidSchema()
            throws ServletException, IOException {
    	when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
    	
        String schemaJson = jsonFromFile("schema/inception.json").toString();
        when(request.getParameter("schema")).thenReturn(schemaJson);

        command.doPost(request, response);

        assertTrue(writer.toString().contains("\"ok\""));
    }
    
    @Test
    public void testInvalidSchema() throws ServletException, IOException {
    	when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
    	
        String schemaJson = "{\"itemDocuments\":[{\"statementGroups\":[{\"statements\":[]}],"
                +"\"nameDescs\":[]}],\"wikibasePrefix\":\"http://www.wikidata.org/entity/\"}";
        
        when(request.getParameter("schema")).thenReturn(schemaJson);
        command.doPost(request, response);
        
        assertTrue(writer.toString().contains("\"error\""));
    }
    
    @Test
    public void testCsrfProtection() throws ServletException, IOException {
    	String schemaJson = jsonFromFile("schema/inception.json").toString();
        when(request.getParameter("schema")).thenReturn(schemaJson);
        
    	command.doPost(request, response);
    	TestUtils.assertEqualAsJson("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}", writer.toString());
    }
}
