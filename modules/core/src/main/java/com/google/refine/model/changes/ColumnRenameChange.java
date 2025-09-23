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

package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ColumnRenameChange extends ColumnChange {

    final protected String _oldColumnName;
    final protected String _newColumnName;

    public ColumnRenameChange(String oldColumnName, String newColumnName) {
        _oldColumnName = oldColumnName;
        _newColumnName = newColumnName;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _oldColumnName);
            project.columnModel.getColumnByName(_oldColumnName).setName(_newColumnName);
            project.columnModel.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _newColumnName);
            project.columnModel.getColumnByName(_newColumnName).setName(_oldColumnName);
            project.columnModel.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("oldColumnName=");
        writer.write(escape(_oldColumnName));
        writer.write('\n');
        writer.write("newColumnName=");
        writer.write(escape(_newColumnName));
        writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String oldColumnName = null;
        String newColumnName = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            if (equal == -1) {
                // This might be a continuation line from corrupted legacy data
                continue;
            }
            
            String field = line.substring(0, equal);
            String value = line.substring(equal + 1);

            if ("oldColumnName".equals(field)) {
                oldColumnName = processFieldValue(value, reader);
            } else if ("newColumnName".equals(field)) {
                newColumnName = processFieldValue(value, reader);
            }
        }

        return new ColumnRenameChange(oldColumnName, newColumnName);
    }

    /**
     * Process a field value, handling both new escaped format and legacy format.
     * For legacy format, handle potential corruption from newline characters.
     */
    private static String processFieldValue(String value, LineNumberReader reader) throws IOException {
        // Check if this is the new escaped format
        if (containsEscapeSequences(value)) {
            return unescape(value);
        } else {
            // Legacy format - might be corrupted if original value contained newlines
            return handleLegacyValue(value, reader);
        }
    }

    /**
     * Handle potentially corrupted legacy values by reading additional lines
     * until we find a proper field or end marker.
     */
    private static String handleLegacyValue(String initialValue, LineNumberReader reader) throws IOException {
        StringBuilder result = new StringBuilder(initialValue);
        
        // Mark current position in case we need to reset
        reader.mark(4096);
        String nextLine;
        
        while ((nextLine = reader.readLine()) != null) {
            // Check if this line looks like a proper field or end marker
            if ("/ec/".equals(nextLine) || 
                (nextLine.contains("=") && 
                 (nextLine.startsWith("oldColumnName=") || nextLine.startsWith("newColumnName=")))) {
                // This is a proper line, reset reader to before this line
                reader.reset();
                break;
            } else {
                // This might be a continuation of the corrupted value
                result.append("\n").append(nextLine);
                reader.mark(4096); // Mark after each line we consume
            }
        }
        
        return result.toString();
    }

    /**
     * Check if a value contains escape sequences that indicate new format
     */
    private static boolean containsEscapeSequences(String value) {
        return value.contains("\\n")
            || value.contains("\\r")
            || value.contains("\\t")
            || value.contains("\\b")
            || value.contains("\\f")
            || value.matches(".*\\\\u[0-9a-fA-F]{4}.*")
            || value.contains("\\\\");
    }

    /**
     * Escape special characters for safe serialization
     */
    private static String escape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Unescape special characters during deserialization
     */
    private static String unescape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            String hex = s.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 5;
                        }
                        break;
                    case '\\': sb.append('\\'); i++; break;
                    default: sb.append(next); i++; break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Getters for testing
    public String getOldColumnName() {
        return _oldColumnName;
    }

    public String getNewColumnName() {
        return _newColumnName;
    }
}
