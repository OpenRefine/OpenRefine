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

package com.google.refine.importers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class ImporterUtilities {

    static public Serializable parseCellValue(String text) {
        if (text.length() > 0) {
            if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")) {
                return text.substring(1, text.length() - 1);
            }

            String text2 = text.trim();
            if (text2.length() > 0) {
                try {
                    return Long.parseLong(text2);
                } catch (NumberFormatException e) {
                }
    
                try {
                    double d = Double.parseDouble(text2);
                    if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                        return d;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return text;
    }

    static public int getIntegerOption(String name, Properties options, int def) {
        int value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        return value;
    }

    static public boolean getBooleanOption(String name, Properties options, boolean def) {
        boolean value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = s.equalsIgnoreCase("on") || s.equals("1") || Boolean.parseBoolean(s);
            } catch (Exception e) {
            }
        }
        return value;
    }

    static public void appendColumnName(List<String> columnNames, int index, String name) {
        name = name.trim();

        while (columnNames.size() <= index) {
            columnNames.add("");
        }

        if (!name.isEmpty()) {
            String oldName = columnNames.get(index);
            if (!oldName.isEmpty()) {
                name = oldName + " " + name;
            }

            columnNames.set(index, name);
        }
    }

    static public void ensureColumnsInRowExist(List<String> columnNames, Row row) {
        int count = row.cells.size();
        while (count > columnNames.size()) {
            columnNames.add("");
        }
    }

    static public void setupColumns(Project project, List<String> columnNames) {
        Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
        for (int c = 0; c < columnNames.size(); c++) {
            String cell = columnNames.get(c).trim();
            if (cell.isEmpty()) {
                cell = "Column";
            } else if (cell.startsWith("\"") && cell.endsWith("\"")) {
                cell = cell.substring(1, cell.length() - 1).trim(); //FIXME is trimming quotation marks appropriate?
            }

            if (nameToIndex.containsKey(cell)) {
                int index = nameToIndex.get(cell);
                nameToIndex.put(cell, index + 1);

                cell = cell.contains(" ") ? (cell + " " + index) : (cell + index);
            } else {
                nameToIndex.put(cell, 2);
            }

            Column column = new Column(c, cell);

            project.columnModel.columns.add(column);
        }
    }

}
