/*
 * 
 * Copyright 2010, Google Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.google.refine.commands.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.CharMatcher;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class SetProjectTagsCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        response.setHeader("Content-Type", "application/json");

        Project project;
        try {
            project = getProject(request);
        } catch (ServletException e) {
            respond(response, "error", e.getLocalizedMessage());
            return;
        }

        ProjectMetadata metadata = project.getMetadata();

        String oldT = request.getParameter("old");
        String newT = request.getParameter("new");

        Map<String, Integer> allProjectTags = ProjectManager.singleton.getAllProjectTags();

        // Lets remove the old tags from the general map
        String[] oldTags = oldT.split(",");
        for (String tag : oldTags) {
            if (allProjectTags != null && allProjectTags.containsKey(tag)) {
                int occurrence = allProjectTags.get(tag);

                if (occurrence == 1) {
                    allProjectTags.remove(tag);
                } else {
                    allProjectTags.put(tag, occurrence - 1);
                }
            }
        }

        // Lets add the new tags to the general map
        String[] newTags = newT.split(" |\\,");
        List<String> polishedTags = new ArrayList<String>(newTags.length);
        for (String tag : newTags) {
            tag = CharMatcher.whitespace().trimFrom(tag);

            if (!tag.isEmpty()) {
                if (allProjectTags != null) {
                    if (allProjectTags.containsKey(tag)) {
                        allProjectTags.put(tag, allProjectTags.get(tag) + 1);
                    } else {
                        allProjectTags.put(tag, 1);
                    }
                }
                polishedTags.add(tag);
            }
        }

        // Lets update the project tags
        metadata.setTags(polishedTags.toArray(new String[polishedTags.size()]));
        metadata.updateModified();

        respond(response, "{ \"code\" : \"ok\" }");
    }
}
