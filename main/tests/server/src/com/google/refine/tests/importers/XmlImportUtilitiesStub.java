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

package com.google.refine.tests.importers;

import java.io.Serializable;
import java.util.List;

import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.ImportParameters;
import com.google.refine.importers.tree.ImportRecord;
import com.google.refine.importers.tree.TreeReader;
import com.google.refine.importers.tree.XmlImportUtilities;
import com.google.refine.model.Project;

public class XmlImportUtilitiesStub extends XmlImportUtilities {
    
    public List<String> detectRecordElementWrapper(TreeReader parser, String tag) throws Exception{
        return super.detectRecordElement(parser, tag);
    }

    public void ProcessSubRecordWrapper(Project project, TreeReader parser, ImportColumnGroup columnGroup,
            ImportRecord record, int level, ImportParameters parameter)
            throws Exception {
        super.processSubRecord(project, parser, columnGroup, record, level, parameter);
    }

    public void findRecordWrapper(Project project, TreeReader parser, String[] recordPath, int pathIndex,
            ImportColumnGroup rootColumnGroup, boolean trimStrings, boolean storeEmptyStrings, boolean guessDataType)
            throws Exception {
        super.findRecord(project, parser, recordPath, pathIndex, rootColumnGroup, -1, 
                new ImportParameters(trimStrings, storeEmptyStrings, guessDataType));
    }

    public void processRecordWrapper(Project project, TreeReader parser, ImportColumnGroup rootColumnGroup,
            boolean trimStrings, boolean storeEmptyStrings, boolean guessDataType)
            throws Exception {
        super.processRecord(project, parser, rootColumnGroup, 
                new ImportParameters(trimStrings, storeEmptyStrings, guessDataType));
    }    

    public void addCellWrapper(Project project, ImportColumnGroup columnGroup, ImportRecord record, String columnLocalName, Serializable value, int commonStartingRowIndex) {
        super.addCell(project, columnGroup, record, columnLocalName, value);
    }

    public void addCellWrapper(Project project, ImportColumnGroup columnGroup, ImportRecord record, String columnLocalName, String text, int commonStartingRowIndex, boolean trimStrings, boolean storeEmptyStrings) {
        super.addCell(project, columnGroup, record, columnLocalName, text, trimStrings, storeEmptyStrings);
    }
}
