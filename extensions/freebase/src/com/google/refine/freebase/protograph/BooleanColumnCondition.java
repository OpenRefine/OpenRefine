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

package com.google.refine.freebase.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;


public class BooleanColumnCondition implements Condition {
    final public String columnName;
    
    public BooleanColumnCondition(String columnName) {
        this.columnName = columnName;
    }
    
    @Override
    public boolean test(Project project, int rowIndex, Row row) {
        Column column = project.columnModel.getColumnByName(columnName);
        if (column != null) {
            Object o = row.getCellValue(column.getCellIndex());
            if (o != null) {
                if (o instanceof Boolean) {
                    return ((Boolean) o).booleanValue();
                } else {
                    return Boolean.parseBoolean(o.toString());
                }
            }
        }
        return false;
    }

    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("columnName"); writer.value(columnName);
        writer.endObject();
    }
}
