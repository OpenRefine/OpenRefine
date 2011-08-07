/*

Copyright 2011, Google Inc.
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

package com.google.refine.extension.gdata;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.gdata.client.DocumentQuery;
import com.google.gdata.client.Query;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.Category;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Person;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.util.ServiceException;

import com.google.refine.RefineServlet;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.ImportingController;
import com.google.refine.util.ParsingUtilities;

public class GDataImportingController implements ImportingController {

    protected RefineServlet servlet;
    
    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        Properties parameters = ParsingUtilities.parseUrlParameters(request);
        String subCommand = parameters.getProperty("subCommand");
        if ("list-documents".equals(subCommand)) {
            doListDocuments(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }
    }

    private void doListDocuments(HttpServletRequest request, HttpServletResponse response, Properties parameters)
        throws ServletException, IOException {

        String token = TokenCookie.getToken(request);
        if (token == null) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }
        
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("documents");
            writer.array();
            
            try {
                DocsService service = getDocsService(token);
                DocumentQuery query = new DocumentQuery(
                    new URL("https://docs.google.com/feeds/default/private/full"));
                query.addCategoryFilter(new Query.CategoryFilter(new Category(
                    "http://schemas.google.com/g/2005#kind",
                    "http://schemas.google.com/docs/2007#spreadsheet")));
                query.setMaxResults(100);
                
                DocumentListFeed feed = service.getFeed(query, DocumentListFeed.class);
                for (DocumentListEntry entry : feed.getEntries()) {
                    writer.object();
                    writer.key("docId"); writer.value(entry.getDocId());
                    writer.key("docLink"); writer.value(entry.getDocumentLink().getHref());
                    writer.key("title"); writer.value(entry.getTitle().getPlainText());
                    writer.key("isViewed"); writer.value(entry.isViewed());
                    writer.key("isStarred"); writer.value(entry.isStarred());
                    
                    DateTime edited = entry.getEdited();
                    if (edited != null) {
                        writer.key("edited"); writer.value(edited.toStringRfc822());
                    }
                    
                    DateTime lastViewed = entry.getLastViewed();
                    if (lastViewed != null) {
                        writer.key("lastViewed"); writer.value(lastViewed.toStringRfc822());
                    }
                    
                    writer.key("authors"); writer.array();
                    for (Person person : entry.getAuthors()) {
                        writer.value(person.getName());
                    }
                    writer.endArray();
                    
                    writer.endObject();
                }
            } catch (ServiceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        } finally {
            w.flush();
            w.close();
        }
    }
    
    private DocsService getDocsService(String token) {
        DocsService service = new DocsService(GDataExtension.SERVICE_APP_NAME);
        service.setAuthSubToken(token);
        return service;
    }
}
