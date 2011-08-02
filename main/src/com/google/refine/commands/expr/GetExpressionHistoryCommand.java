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

package com.google.refine.commands.expr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.preference.TopList;

public class GetExpressionHistoryCommand extends Command {

    static protected List<String> toExpressionList(Object o) {
        return o == null ? new ArrayList<String>() : ((TopList) o).getList();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            List<String> localExpressions = toExpressionList(project.getMetadata().getPreferenceStore().get("scripting.expressions"));
            localExpressions = localExpressions.size() > 20 ? localExpressions.subList(0, 20) : localExpressions;
            
            List<String> globalExpressions = toExpressionList(ProjectManager.singleton.getPreferenceStore().get("scripting.expressions"));
            Set<String> starredExpressions = new HashSet<String>(((TopList)ProjectManager.singleton.getPreferenceStore().get("scripting.starred-expressions")).getList());
            
            Set<String> done = new HashSet<String>();
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("expressions");
                writer.array();
                for (String s : localExpressions) {
                    writer.object();
                    writer.key("code"); writer.value(s);
                    writer.key("global"); writer.value(false);
                    writer.key("starred"); writer.value(starredExpressions.contains(s));
                    writer.endObject();
                    done.add(s);
                }
                for (String s : globalExpressions) {
                    if (!done.contains(s)) {
                        writer.object();
                        writer.key("code"); writer.value(s);
                        writer.key("global"); writer.value(true);
                        writer.key("starred"); writer.value(starredExpressions.contains(s));
                        writer.endObject();
                    }
                }
                writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
