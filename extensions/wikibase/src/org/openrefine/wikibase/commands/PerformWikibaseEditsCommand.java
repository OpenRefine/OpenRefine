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

package org.openrefine.wikibase.commands;

import javax.servlet.http.HttpServletRequest;

import org.openrefine.wikibase.operations.PerformWikibaseEditsOperation;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class PerformWikibaseEditsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project, HttpServletRequest request, EngineConfig engineConfig)
            throws Exception {
        String summary = request.getParameter("summary");
        String maxlagStr = request.getParameter("maxlag");
        int maxlag = maxlagStr == null ? 5 : Integer.parseInt(maxlagStr);
        String maxEditsPerMinuteStr = request.getParameter("maxEditsPerMinute");
        Integer maxEditsPerMinute = maxEditsPerMinuteStr == null ? null : Integer.parseInt(maxEditsPerMinuteStr);
        String tag = request.getParameter("tag");
        String editGroupsUrlSchema = request.getParameter("editGroupsUrlSchema");
        return new PerformWikibaseEditsOperation(engineConfig, summary, maxlag, editGroupsUrlSchema, maxEditsPerMinute, tag);
    }

}
