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

package com.google.refine.browsing;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Interface for visiting rows one by one. The rows visited are only those that match some particular criteria, such as
 * facets' constraints.
 */
public interface RowVisitor {

    /**
     * Called before any visit() call.
     * 
     * @param project
     */
    public void start(Project project);

    /**
     * @param project
     *            project the row is part of
     * @param rowIndex
     *            zero-based row index (unaffected by a temporary sort)
     * @param row
     *            row
     * @return true to abort visitation early - no further visit calls will be made
     * @deprecated use {@link #visit(Project, int, int, Row)}
     */
    @Deprecated
    public boolean visit(
            Project project,
            int rowIndex,
            Row row);

    /**
     * @param project
     *            project the row is part of
     * @param rowIndex
     *            zero-based row index (unaffected by a temporary sort)
     * @param sortedRowIndex
     *            zero-based row index in the sorted view (or equal to rowIndex if no temporary sorting is in place)
     * @param row
     *            row
     * @return true to abort visitation early - no further visit calls will be made
     */
    public default boolean visit(
            Project project,
            int rowIndex,
            int sortedRowIndex,
            Row row) {
        return visit(project, rowIndex, row);
    }

    /**
     * Called after all visit() calls.
     * 
     * @param project
     */
    public void end(Project project);
}
