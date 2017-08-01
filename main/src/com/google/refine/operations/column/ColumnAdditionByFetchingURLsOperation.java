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

package com.google.refine.operations.column;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.WrappedCell;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.model.changes.ColumnAdditionChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OnError;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.TextTransformOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheLoader;

public class ColumnAdditionByFetchingURLsOperation extends EngineDependentOperation {
    final protected String     _baseColumnName;
    final protected String     _urlExpression;
    final protected OnError    _onError;

    final protected String     _newColumnName;
    final protected int        _columnInsertIndex;
    final protected int        _delay;
    final protected boolean    _cacheResponses;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");

        return new ColumnAdditionByFetchingURLsOperation(
            engineConfig,
            obj.getString("baseColumnName"),
            obj.getString("urlExpression"),
            TextTransformOperation.stringToOnError(obj.getString("onError")),
            obj.getString("newColumnName"),
            obj.getInt("columnInsertIndex"),
            obj.getInt("delay"),
            obj.optBoolean("cacheResponses", false) // false for retro-compatibility
        );
    }

    public ColumnAdditionByFetchingURLsOperation(
        JSONObject     engineConfig,
        String         baseColumnName,
        String         urlExpression,
        OnError        onError,
        String         newColumnName,
        int            columnInsertIndex,
        int            delay,
        boolean        cacheResponses
    ) {
        super(engineConfig);

        _baseColumnName = baseColumnName;
        _urlExpression = urlExpression;
        _onError = onError;

        _newColumnName = newColumnName;
        _columnInsertIndex = columnInsertIndex;

        _delay = delay;
        _cacheResponses = cacheResponses;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("newColumnName"); writer.value(_newColumnName);
        writer.key("columnInsertIndex"); writer.value(_columnInsertIndex);
        writer.key("baseColumnName"); writer.value(_baseColumnName);
        writer.key("urlExpression"); writer.value(_urlExpression);
        writer.key("onError"); writer.value(TextTransformOperation.onErrorToString(_onError));
        writer.key("delay"); writer.value(_delay);
        writer.key("cacheResponses"); writer.value(_cacheResponses);
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Create column " + _newColumnName +
            " at index " + _columnInsertIndex +
            " by fetching URLs based on column " + _baseColumnName +
            " using expression " + _urlExpression;
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return "Create new column " + _newColumnName +
            ", filling " + cellsAtRows.size() +
            " rows by fetching URLs based on column " + column.getName() +
            " and formulated as " + _urlExpression;
    }


    @Override
    public Process createProcess(Project project, Properties options) throws Exception {
        Engine engine = createEngine(project);
        engine.initializeFromJSON(_engineConfig);

        Evaluable eval = MetaParser.parse(_urlExpression);

        return new ColumnAdditionByFetchingURLsProcess(
            project,
            engine,
            eval,
            getBriefDescription(null),
            _cacheResponses
        );
    }

    public class ColumnAdditionByFetchingURLsProcess extends LongRunningProcess implements Runnable {
        final protected Project       _project;
        final protected Engine        _engine;
        final protected Evaluable     _eval;
        final protected long          _historyEntryID;
        protected int                 _cellIndex;
        protected LoadingCache<String, Serializable> _urlCache;

        public ColumnAdditionByFetchingURLsProcess(
            Project project,
            Engine engine,
            Evaluable eval,
            String description,
            boolean cacheResponses
        ) throws JSONException {
            super(description);
            _project = project;
            _engine = engine;
            _eval = eval;
            _historyEntryID = HistoryEntry.allocateID();
            _urlCache = null;
            if (cacheResponses) {
                _urlCache = CacheBuilder.newBuilder()
                .maximumSize(2048)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                     new CacheLoader<String, Serializable>() {
                        public Serializable load(String urlString) throws Exception {
                            Serializable result = fetch(urlString);
                            try {
                                // Always sleep for the delay, no matter how long the
                                // request took. This is more responsible than substracting
                                // the time spend requesting the URL, because it naturally
                                // slows us down if the server is busy and takes a long time
                                // to reply.
                                if (_delay > 0) {
                                    Thread.sleep(_delay);
                                }
                            } catch (InterruptedException e) {
                                result = null;
                            }

			    if (result == null) {
				// the load method should not return any null value
				throw new Exception("null result returned by fetch");
			    }
                            return result;
                        }
                     });
            }
        }

        @Override
        public void write(JSONWriter writer, Properties options)
                throws JSONException {

            writer.object();
            writer.key("id"); writer.value(hashCode());
            writer.key("description"); writer.value(_description);
            writer.key("immediate"); writer.value(false);
            writer.key("status"); writer.value(_thread == null ? "pending" : (_thread.isAlive() ? "running" : "done"));
            writer.key("progress"); writer.value(_progress);
            writer.endObject();
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }

        @Override
        public void run() {
            Column column = _project.columnModel.getColumnByName(_baseColumnName);
            if (column == null) {
                _project.processManager.onFailedProcess(this, new Exception("No column named " + _baseColumnName));
                return;
            }
            if (_project.columnModel.getColumnByName(_newColumnName) != null) {
                _project.processManager.onFailedProcess(this, new Exception("Another column already named " + _newColumnName));
                return;
            }

            List<CellAtRow> urls = new ArrayList<CellAtRow>(_project.rows.size());

            FilteredRows filteredRows = _engine.getAllFilteredRows();
            filteredRows.accept(_project, createRowVisitor(urls));

            List<CellAtRow> responseBodies = new ArrayList<CellAtRow>(urls.size());
            for (int i = 0; i < urls.size(); i++) {
                CellAtRow urlData = urls.get(i);
                String urlString = urlData.cell.value.toString();

                Serializable response = null;
                if (_urlCache != null) {
                    response = cachedFetch(urlString);
                } else {
                    response = fetch(urlString);
                }

                if (response != null) {
                    CellAtRow cellAtRow = new CellAtRow(
                            urlData.row,
                            new Cell(response, null));

                    responseBodies.add(cellAtRow);
                }

                _progress = i * 100 / urls.size();

                if (_canceled) {
                    break;
                }
            }

            if (!_canceled) {
                HistoryEntry historyEntry = new HistoryEntry(
                    _historyEntryID,
                    _project,
                    _description,
                    ColumnAdditionByFetchingURLsOperation.this,
                    new ColumnAdditionChange(
                        _newColumnName,
                        _columnInsertIndex,
                        responseBodies)
                );

                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }

        Serializable cachedFetch(String urlString) {
            try {
                return  _urlCache.get(urlString);
            } catch(Exception e) {
                return null;
            }
        }

        Serializable fetch(String urlString) {
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                return null;
            }

            try {
                URLConnection urlConnection = url.openConnection();
//                    urlConnection.setRequestProperty(_headerKey, _headerValue);

                try {
                    InputStream is = urlConnection.getInputStream();
                    try {
                        String encoding = urlConnection.getContentEncoding();
                        if (encoding == null) {
                            String contentType = urlConnection.getContentType();
                            if (contentType != null) {
                                final String charsetEqual = "charset=";
                                int c = contentType.lastIndexOf(charsetEqual);
                                if (c > 0) {
                                    encoding = contentType.substring(c + charsetEqual.length());
                                }
                            }
                        }
                        return ParsingUtilities.inputStreamToString(
                                                is, (encoding == null) || ( encoding.equalsIgnoreCase("\"UTF-8\"")) ? "UTF-8" : encoding);

                    } finally {
                        is.close();
                    }
                } catch (IOException e) {
                    String message;
                    if (urlConnection instanceof HttpURLConnection) {
                        int status = ((HttpURLConnection)urlConnection).getResponseCode();
                        String errorString = "";
                        InputStream errorStream = ((HttpURLConnection)urlConnection).getErrorStream();
                        if (errorStream != null) {
                            errorString = ParsingUtilities.inputStreamToString(errorStream);
                        }
                        message = String.format("HTTP error %d : %s | %s",status,
                                ((HttpURLConnection)urlConnection).getResponseMessage(),
                                errorString);
                    } else {
                        message = e.toString();
                    }
                    return _onError == OnError.StoreError ?
                            new EvalError(message) : null;
                }
            } catch (Exception e) {
                return _onError == OnError.StoreError ?
                        new EvalError(e.getMessage()) : null;
            }
        }

        RowVisitor createRowVisitor(List<CellAtRow> cellsAtRows) {
            return new RowVisitor() {
                int              cellIndex;
                Properties       bindings;
                List<CellAtRow>  cellsAtRows;

                public RowVisitor init(List<CellAtRow> cellsAtRows) {
                    Column column = _project.columnModel.getColumnByName(_baseColumnName);

                    this.cellIndex = column.getCellIndex();
                    this.bindings = ExpressionUtils.createBindings(_project);
                    this.cellsAtRows = cellsAtRows;
                    return this;
                }

                @Override
                public void start(Project project) {
                    // nothing to do
                }

                @Override
                public void end(Project project) {
                    // nothing to do
                }

                @Override
                public boolean visit(Project project, int rowIndex, Row row) {
                    Cell cell = row.getCell(cellIndex);
                    Cell newCell = null;

                    ExpressionUtils.bind(bindings, row, rowIndex, _baseColumnName, cell);

                    Object o = _eval.evaluate(bindings);
                    if (o != null) {
                        if (o instanceof Cell) {
                            newCell = (Cell) o;
                        } else if (o instanceof WrappedCell) {
                            newCell = ((WrappedCell) o).cell;
                        } else {
                            Serializable v = ExpressionUtils.wrapStorable(o);
                            if (ExpressionUtils.isNonBlankData(v)) {
                                newCell = new Cell(v.toString(), null);
                            }
                        }
                    }

                    if (newCell != null) {
                        cellsAtRows.add(new CellAtRow(rowIndex, newCell));
                    }

                    return false;
                }
            }.init(cellsAtRows);
        }
    }
}
