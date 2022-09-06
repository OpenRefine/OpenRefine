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

package com.google.refine.model;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.google.refine.history.HistoryEntry;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.OperationResolver;
import com.google.refine.process.Process;
import com.google.refine.process.QuickHistoryEntryProcess;

/*
 *  An abstract operation can be applied to different but similar
 *  projects.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "op", visible = true) // for
                                                                                                                 // UnknownOperation,
                                                                                                                 // which
                                                                                                                 // needs
                                                                                                                 // to
                                                                                                                 // read
                                                                                                                 // its
                                                                                                                 // own
                                                                                                                 // id
@JsonTypeIdResolver(OperationResolver.class)
abstract public class AbstractOperation {

    public Process createProcess(Project project, Properties options) throws Exception {
        return new QuickHistoryEntryProcess(project, getBriefDescription(null)) {

            @Override
            protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
                return AbstractOperation.this.createHistoryEntry(_project, historyEntryID);
            }
        };
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected String getBriefDescription(Project project) {
        throw new UnsupportedOperationException();
    }

    @JsonIgnore // the operation id is already added as "op" by the JsonTypeInfo annotation
    public String getOperationId() {
        return OperationRegistry.s_opClassToName.get(this.getClass());
    }

    @JsonProperty("description")
    public String getJsonDescription() {
        return getBriefDescription(null);
    }
}
