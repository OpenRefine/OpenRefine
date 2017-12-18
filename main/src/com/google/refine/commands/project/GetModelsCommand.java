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

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.MetaParser.LanguageInfo;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

public class GetModelsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalRespond(request, response);
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        internalRespond(request, response);
    }
    
    protected void internalRespond(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        
        Project project = null;
        
        // This command also supports retrieving rows for an importing job.
        String importingJobID = request.getParameter("importingJobID");
        if (importingJobID != null) {
            long jobID = Long.parseLong(importingJobID);
            ImportingJob job = ImportingManager.getJob(jobID);
            if (job != null) {
                project = job.project;
            }
        }
        if (project == null) {
            project = getProject(request);
        }
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            response.setHeader("Cache-Control", "no-cache");
            
            Properties options = new Properties();
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            writer.key("columnModel"); project.columnModel.write(writer, options);
            writer.key("recordModel"); project.recordModel.write(writer, options);
            
            writer.key("overlayModels"); writer.object();
            for (String modelName : project.overlayModels.keySet()) {
                OverlayModel overlayModel = project.overlayModels.get(modelName);
                if (overlayModel != null) {
                    writer.key(modelName);
                    
                    project.overlayModels.get(modelName).write(writer, options);
                }
            }
            writer.endObject();
            
            writer.key("scripting"); writer.object();
            for (String languagePrefix : MetaParser.getLanguagePrefixes()) {
                LanguageInfo info = MetaParser.getLanguageInfo(languagePrefix);
                
                writer.key(languagePrefix);
                writer.object();
                    writer.key("name"); writer.value(info.name);
                    writer.key("defaultExpression"); writer.value(info.defaultExpression);
                writer.endObject();
            }
            writer.endObject();
            
            writer.endObject();
        } catch (JSONException e) {
            HttpUtilities.respondException(response, e);
        }
    }

}
