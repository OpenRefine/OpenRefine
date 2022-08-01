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

package com.google.refine.templating;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

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

    public RowVisitor getRowVisitor(Writer writer, int limit) {
        return get(writer, limit);
    }

    public RecordVisitor getRecordVisitor(Writer writer, int limit) {
        return get(writer, limit);
    }

    protected RowWritingVisitor get(Writer writer, int limit) {
        return new RowWritingVisitor(writer, limit);
    }

    protected class RowWritingVisitor implements RowVisitor, RecordVisitor {

        final protected int limit;
        final protected Writer writer;
        protected Properties bindings;

        public int total;
        public int totalRows;

        public RowWritingVisitor(Writer writer, int limit) {
            this.limit = limit;
            this.writer = writer;
        }

        @Override
        public void start(Project project) {
            bindings = ExpressionUtils.createBindings(project);

            try {
                if (_prefix != null) {
                    writer.write(_prefix);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void end(Project project) {
            try {
                if (_suffix != null) {
                    writer.write(_suffix);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            if (limit <= 0 || total < limit) {
                internalVisit(project, rowIndex, row);
            }
            total++;

            return limit > 0 && total >= limit;
        }

        @Override
        public boolean visit(Project project, Record record) {
            if (limit <= 0 || total < limit) {
                internalVisit(project, record);
            }
            total++;

            return limit > 0 && total >= limit;
        }

        protected void writeValue(Object v) throws IOException {
            if (v == null) {
                writer.write("null");
            } else if (ExpressionUtils.isError(v)) {
                writer.write("null");
                // writer.write("[Error: " + ((EvalError) v).message);
            } else if (v instanceof String) {
                writer.write((String) v);
            } else {
                writer.write(v.toString());
            }
        }

        /**
         * This method is modified for issue 3955 Issue link: https://github.com/OpenRefine/OpenRefine/issues/3955 The
         * modification is to use the new variable totalRows instead of total
         */
        public boolean internalVisit(Project project, int rowIndex, Row row) {
            try {
                if (totalRows > 0 && _separator != null) {
                    writer.write(_separator);
                }
                totalRows++;
                ExpressionUtils.bind(bindings, row, rowIndex, null, null);
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
                                    writeValue(v);
                                }
                            } else {
                                Collection<Object> a = ExpressionUtils.toObjectCollection(value);
                                for (Object v : a) {
                                    writeValue(v);
                                }
                            }
                            continue;
                        }

                        writeValue(value);
                    }
                }
            } catch (IOException e) {
                // ignore
            }
            return false;
        }

        protected boolean internalVisit(Project project, Record record) {
            bindings.put("recordIndex", record.recordIndex);

            for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
                Row row = project.rows.get(r);

                bindings.put("rowIndex", r);

                internalVisit(project, r, row);

                bindings.remove("recordIndex");
            }
            return false;
        }
    }

}
