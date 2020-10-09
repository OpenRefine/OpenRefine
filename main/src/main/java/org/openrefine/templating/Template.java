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

package org.openrefine.templating;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

public class Template {
    protected String _prefix;
    protected String _suffix;
    protected String _separator;

    protected List<Fragment> _fragments;

    public Template(List<Fragment> fragments) {
        _fragments = fragments;
    }

    public void setPrefix(String prefix) {
        _prefix = prefix;
    }

    public void setSuffix(String suffix) {
        _suffix = suffix;
    }

    public void setSeparator(String separator) {
        _separator = separator;
    }
    
    public void writeRows(Iterable<IndexedRow> rows, Writer writer, ColumnModel columnModel, int limit) throws IOException {
    	Properties bindings = ExpressionUtils.createBindings();
    	if (_prefix != null) {
            writer.write(_prefix);
        }
    	long total = 0;
    	for(IndexedRow indexedRow : rows) {
    		if (limit > 0 && total >= limit) {
    			break;
    		}
    		
    		internalVisit(indexedRow.getIndex(), indexedRow.getRow(), total, writer, bindings, columnModel);
    		total++;
    	}
    	if (_suffix != null) {
            writer.write(_suffix);
        }
    }
    
    public void writeRecords(Iterable<Record> records, Writer writer, ColumnModel columnModel, int limit) throws IOException {
    	Properties bindings = ExpressionUtils.createBindings();
    	if (_prefix != null) {
            writer.write(_prefix);
        }
    	long total = 0;
    	for(Record record : records) {
    		if (limit > 0 && total >= limit) {
    			break;
    		}
    		bindings.put("recordIndex", record.getStartRowId());
    		for (IndexedRow indexedRow : record.getIndexedRows()) {
    			if (limit > 0 && total >= limit) {
        			break;
        		}
    			internalVisit(indexedRow.getIndex(), indexedRow.getRow(), total, writer, bindings, columnModel);
    			bindings.remove("recordIndex");
    			total++;
    		}
    	}
    	if (_suffix != null) {
            writer.write(_suffix);
        }
    }
    
    public void internalVisit(long rowIndex, Row row, long total, Writer writer, Properties bindings, ColumnModel columnModel) throws IOException {
        if (total > 0 && _separator != null) {
            writer.write(_separator);
        }

        ExpressionUtils.bind(bindings, columnModel, row, rowIndex, null, null);
        for (Fragment f : _fragments) {
            if (f instanceof StaticFragment) {
                writer.write(((StaticFragment) f).text);
            } else {
                DynamicFragment df = (DynamicFragment) f;
                Object value = df.eval.evaluate(bindings);

                if (value != null && ExpressionUtils.isArrayOrCollection(value)) {
                    if (ExpressionUtils.isArray(value)) {
                        Object[] a = (Object[]) value;
                        for (Object v : a) {
                            writeValue(v, writer);
                        }
                    } else {
                        Collection<Object> a = ExpressionUtils.toObjectCollection(value);
                        for (Object v : a) {
                            writeValue(v, writer);
                        }
                    }
                    continue;
                }

                writeValue(value, writer);
            }
        }
    }
    
    protected void writeValue(Object v, Writer writer) throws IOException {
        if (v == null) {
            writer.write("null");
        } else if (ExpressionUtils.isError(v)) {
            writer.write("null");
            //writer.write("[Error: " + ((EvalError) v).message);
        } else if (v instanceof String) {
            writer.write((String) v);
        } else {
            writer.write(v.toString());
        }
    }

}
