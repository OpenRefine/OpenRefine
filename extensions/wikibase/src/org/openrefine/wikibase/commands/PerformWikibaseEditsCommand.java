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

import org.openrefine.browsing.EngineConfig;
import org.openrefine.commands.EngineDependentCommand;
import org.openrefine.model.Project;
import org.openrefine.operations.Operation;
import org.openrefine.wikibase.operations.PerformWikibaseEditsOperation;

public class PerformWikibaseEditsCommand extends EngineDependentCommand {

    @Override
    protected Operation createOperation(Project project, HttpServletRequest request, EngineConfig engineConfig)
            throws Exception {
        String summary = request.getParameter("summary");
        String maxlagStr = request.getParameter("maxlag");
        String batchSizeStr = request.getParameter("batchSize");
        int maxlag = maxlagStr == null ? 5 : Integer.parseInt(maxlagStr);
        int batchSize = batchSizeStr == null ? 50 : Integer.parseInt(batchSizeStr);
        String maxEditsPerMinuteStr = request.getParameter("maxEditsPerMinute");
        Integer maxEditsPerMinute = maxEditsPerMinuteStr == null ? null : Integer.parseInt(maxEditsPerMinuteStr);
        String tag = request.getParameter("tag");
        String editGroupsUrlSchema = request.getParameter("editGroupsUrlSchema");
        return new PerformWikibaseEditsOperation(engineConfig, summary, maxlag, batchSize, editGroupsUrlSchema, maxEditsPerMinute, tag);
    }

}
