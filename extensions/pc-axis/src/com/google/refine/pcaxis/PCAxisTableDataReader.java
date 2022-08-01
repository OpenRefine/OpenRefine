/*

Copyright 2011, Google Inc.
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

package com.google.refine.pcaxis;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.refine.importers.TabularImportingParserBase.TableDataReader;

public class PCAxisTableDataReader implements TableDataReader {

    final private static class Dimension {

        String name;
        List<String> values;
        int next;
    }

    final private LineNumberReader _lnReader;
    final private List<Exception> exceptions;

    String _line = null;
    List<Dimension> _dimensions = null;

    public PCAxisTableDataReader(LineNumberReader lnReader, List<Exception> exceptions) {
        this._lnReader = lnReader;
        this.exceptions = exceptions;
    }

    @Override
    public List<Object> getNextRowOfCells() throws IOException {
        if (_line == null) {
            _line = _lnReader.readLine();
        }
        if (_line == null) {
            return null;
        }

        if (_dimensions == null) {
            return parseMetadataPrologueForColumnNames();
        } else {
            return parseForNextDataRow();
        }
    }

    private List<Object> parseMetadataPrologueForColumnNames() throws IOException {
        _dimensions = new LinkedList<Dimension>();

        List<String> dimensionNames = new ArrayList<String>();
        while (_line != null) {
            int equal = _line.indexOf('=');
            if (equal < 0 || _line.startsWith("DATA=")) {
                break;
            }

            // Save the line in case parseValues() changes it.
            String savedLine = _line;

            List<String> values = parseMetadataValues(equal + 1, exceptions);

            if (savedLine.startsWith("VALUES(\"")) {
                Dimension dimension = new Dimension();
                dimension.name = savedLine.substring(8, equal - 2);
                dimension.values = values;
                _dimensions.add(dimension);
            } else if (savedLine.startsWith("STUB=")) {
                dimensionNames.addAll(0, values);
            } else if (savedLine.startsWith("HEADING=")) {
                dimensionNames.addAll(values);
            }
            _line = _lnReader.readLine();
        }

        final Map<String, Integer> dimensionNameToOrder = new HashMap<String, Integer>();
        for (int i = 0; i < dimensionNames.size(); i++) {
            dimensionNameToOrder.put(dimensionNames.get(i), dimensionNames.size() - i - 1);
        }

        Collections.sort(_dimensions, new Comparator<Dimension>() {

            @Override
            public int compare(Dimension d0, Dimension d1) {
                return dimensionNameToOrder.get(d0.name)
                        .compareTo(dimensionNameToOrder.get(d1.name));
            }
        });

        List<Object> columnNames = new LinkedList<Object>();
        for (int i = _dimensions.size() - 1; i >= 0; i--) {
            columnNames.add(_dimensions.get(i).name);
        }
        columnNames.add("value");

        return columnNames;
    }

    private List<String> parseMetadataValues(int start, List<Exception> _exceptions) throws IOException {
        List<String> values = new ArrayList<String>();
        outer: while (_line != null && start < _line.length()) {
            char c = _line.charAt(start);
            if (c == '"') {
                // A string
                StringBuffer sb = new StringBuffer();
                inner: while (_line != null && start < _line.length()) {
                    int close = _line.indexOf('"', start + 1);
                    if (close < 0) {
                        // Exceptional case of missing closing "
                        _exceptions.add(new Exception(
                                "Missing closing quotation mark on line " + _lnReader.getLineNumber()));

                        sb.append(_line.substring(start + 1));
                        values.add(sb.toString());
                        break outer;
                    } else {
                        sb.append(_line.substring(start + 1, close));
                        if (close == _line.length() - 1) {
                            // String value continues on next line
                            _line = _lnReader.readLine();
                            start = 0;
                            if (_line != null && _line.length() > 0) {
                                c = _line.charAt(0);
                                if (c == '"') {
                                    continue inner;
                                }
                            }
                            break;
                        } else {
                            start = close + 1;
                            break;
                        }
                    }
                }
                values.add(sb.toString());
            } else {
                // A number or identifier
                int comma = customIndexOf(_line, ',', start + 1);
                int semicolon = customIndexOf(_line, ';', start + 1);
                int space = customIndexOf(_line, ' ', start + 1);
                int end = Math.min(comma, Math.min(semicolon, space));
                values.add(_line.substring(start, end));
                start = end;
            }

            if (start == _line.length()) {
                // End of line but no ;. Continue onto next line.
                _line = _lnReader.readLine();
                start = 0;
            } else {
                c = _line.charAt(start);
                if (c == ';' || c == ')') {
                    break;
                } else if (c == ',' || c == ' ' || c == '-') {
                    start++;
                    if (start == _line.length()) {
                        _line = _lnReader.readLine();
                        start = 0;
                    }
                } else {
                    // Exceptional case.
                    _exceptions.add(new Exception(
                            "Unrecognized character " + c + " on line " + _lnReader.getLineNumber()));
                    break;
                }
            }
        }
        return values;
    }

    private List<Object> parseForNextDataRow() throws IOException {
        List<Object> cells = getNextBatchOfDataValues(1);
        if (cells.size() == 0) {
            return null;
        }

        if (_dimensions.size() > 0) {
            for (int i = 0; i < _dimensions.size(); i++) {
                Dimension d = _dimensions.get(i);
                if (d.next == d.values.size()) {
                    d.next = 0;
                    if (i < _dimensions.size() - 1) {
                        _dimensions.get(i + 1).next++;
                    }
                }
                cells.add(0, d.values.get(d.next));
                if (i == 0) {
                    d.next++;
                }
            }
        }

        return cells;
    }

    private List<Object> getNextBatchOfDataValues(int expectedCount) throws IOException {
        List<Object> cells = new LinkedList<Object>();

        int start = _line.startsWith("DATA=") ? 5 : 0;
        while (_line != null) {
            int end = Math.min(
                    customIndexOf(_line, ';', start),
                    Math.min(
                            customIndexOf(_line, ' ', start),
                            customIndexOf(_line, '\t', start)));

            if (end > start) {
                cells.add(_line.substring(start, end));
            }

            while (end < _line.length()) {
                if (Character.isWhitespace(_line.charAt(end))) {
                    end++;
                } else {
                    break;
                }
            }
            if (end == _line.length()) {
                _line = _lnReader.readLine();
                start = 0;
            } else if (_line.charAt(end) == ';') {
                _line = _lnReader.readLine();
                break;
            } else {
                start = end;
            }

            if (cells.size() == expectedCount) {
                _line = _line.substring(start);
                break;
            }
        }
        return cells;
    }

    static private int customIndexOf(String s, char c, int start) {
        int i = s.indexOf(c, start);
        return i < 0 ? s.length() : i;
    }
}
