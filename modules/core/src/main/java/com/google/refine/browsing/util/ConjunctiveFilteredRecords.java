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

package com.google.refine.browsing.util;

import java.util.LinkedList;
import java.util.List;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

/**
 * Encapsulate logic for visiting records that match all given record filters.
 */
public class ConjunctiveFilteredRecords implements FilteredRecords {

    final protected List<RecordFilter> _recordFilters = new LinkedList<RecordFilter>();

    public void add(RecordFilter recordFilter) {
        _recordFilters.add(recordFilter);
    }

    @Override
    public void accept(Project project, RecordVisitor visitor) {
        try {
            visitor.start(project);

            int c = project.recordModel.getRecordCount();
            for (int r = 0; r < c; r++) {
                Record record = project.recordModel.getRecord(r);
                if (matchRecord(project, record)) {
                    if (visitor.visit(project, record)) {
                        return;
                    }
                }
            }
        } finally {
            visitor.end(project);
        }
    }

    protected boolean matchRecord(Project project, Record record) {
        for (RecordFilter recordFilter : _recordFilters) {
            if (!recordFilter.filterRecord(project, record)) {
                return false;
            }
        }
        return true;
    }
}
