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

package org.openrefine.operations.column;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.WrappedCell;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.Column;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.CellAtRow;
import org.openrefine.model.changes.ColumnAdditionChange;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OnError;
import org.openrefine.operations.OperationDescription;
import org.openrefine.process.LongRunningProcess;
import org.openrefine.process.Process;
import org.openrefine.util.HttpClient;

public class ColumnAdditionByFetchingURLsOperation extends EngineDependentOperation {

    public static final class HttpHeader {

        @JsonProperty("name")
        final public String name;
        @JsonProperty("value")
        final public String value;

        @JsonCreator
        public HttpHeader(
                @JsonProperty("name") String name,
                @JsonProperty("value") String value) {
            this.name = name;
            this.value = value;
        }
    }

    final protected String _baseColumnName;
    final protected String _urlExpression;
    final protected OnError _onError;

    final protected String _newColumnName;
    final protected int _columnInsertIndex;
    final protected int _delay;
    final protected boolean _cacheResponses;
    final protected List<HttpHeader> _httpHeadersJson;
    private Header[] httpHeaders = new Header[0];
    private HttpClient _httpClient;

    @JsonCreator
    public ColumnAdditionByFetchingURLsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("urlExpression") String urlExpression,
            @JsonProperty("onError") OnError onError,
            @JsonProperty("newColumnName") String newColumnName,
            @JsonProperty("columnInsertIndex") int columnInsertIndex,
            @JsonProperty("delay") int delay,
            @JsonProperty("cacheResponses") boolean cacheResponses,
            @JsonProperty("httpHeadersJson") List<HttpHeader> httpHeadersJson) {
        super(engineConfig);

        _baseColumnName = baseColumnName;
        _urlExpression = urlExpression;
        _onError = onError;

        _newColumnName = newColumnName;
        _columnInsertIndex = columnInsertIndex;

        _delay = delay;
        _cacheResponses = cacheResponses;
        _httpHeadersJson = httpHeadersJson;

        List<Header> headers = new ArrayList<Header>();
        if (_httpHeadersJson != null) {
            for (HttpHeader header : _httpHeadersJson) {
                if (!isNullOrEmpty(header.name) && !isNullOrEmpty(header.value)) {
                    headers.add(new BasicHeader(header.name, header.value));
                }
            }
        }
        httpHeaders = headers.toArray(httpHeaders);
        _httpClient = new HttpClient(_delay);

    }

    @JsonProperty("newColumnName")
    public String getNewColumnName() {
        return _newColumnName;
    }

    @JsonProperty("columnInsertIndex")
    public int getColumnInsertIndex() {
        return _columnInsertIndex;
    }

    @JsonProperty("baseColumnName")
    public String getBaseColumnName() {
        return _baseColumnName;
    }

    @JsonProperty("urlExpression")
    public String getUrlExpression() {
        return _urlExpression;
    }

    @JsonProperty("onError")
    public OnError getOnError() {
        return _onError;
    }

    @JsonProperty("delay")
    public int getDelay() {
        return _delay;
    }

    @JsonProperty("httpHeadersJson")
    public List<HttpHeader> getHttpHeadersJson() {
        return _httpHeadersJson;
    }

    @JsonProperty("cacheResponses")
    public boolean getCacheResponses() {
        return _cacheResponses;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_addition_by_fetching_urls_brief(_newColumnName, _columnInsertIndex, _baseColumnName,
                _urlExpression);
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return OperationDescription.column_addition_by_fetching_urls_desc(_newColumnName, cellsAtRows.size(), column.getName(),
                _urlExpression);
    }

    @Override
    public Process createProcess(Project project, Properties options) throws Exception {
        Engine engine = createEngine(project);
        engine.initializeFromConfig(_engineConfig);

        Evaluable eval = MetaParser.parse(_urlExpression);

        return new ColumnAdditionByFetchingURLsProcess(
                project,
                engine,
                eval,
                getBriefDescription(null),
                _cacheResponses);
    }

    public class ColumnAdditionByFetchingURLsProcess extends LongRunningProcess implements Runnable {

        final protected Project _project;
        final protected Engine _engine;
        final protected Evaluable _eval;
        final protected long _historyEntryID;
        protected int _cellIndex;
        protected LoadingCache<String, Serializable> _urlCache;

        public ColumnAdditionByFetchingURLsProcess(
                Project project,
                Engine engine,
                Evaluable eval,
                String description,
                boolean cacheResponses) {
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
                                        Serializable result = fetch(urlString, httpHeaders);
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

            int count = urls.size();
            List<CellAtRow> responseBodies = new ArrayList<CellAtRow>(count);
            int i = 0;
            for (CellAtRow urlData : urls) {
                String urlString = urlData.cell.value.toString();

                Serializable response = null;
                if (_urlCache != null) {
                    response = cachedFetch(urlString);
                } else {
                    response = fetch(urlString, httpHeaders);
                }

                if (response != null) {
                    CellAtRow cellAtRow = new CellAtRow(
                            urlData.row,
                            new Cell(response, null));

                    responseBodies.add(cellAtRow);
                }

                _progress = i++ * 100 / count;

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
                                responseBodies));

                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }

        Serializable cachedFetch(String urlString) {
            try {
                return _urlCache.get(urlString);
            } catch (Exception e) {
                return null;
            }
        }

        Serializable fetch(String urlString, Header[] headers) {
            try { // HttpClients.createDefault()) {
                try {
                    return _httpClient.getAsString(urlString, headers);
                } catch (IOException e) {
                    return _onError == OnError.StoreError ? new EvalError(e) : null;
                }
            } catch (Exception e) {
                return _onError == OnError.StoreError ? new EvalError(e.getMessage()) : null;
            }
        }

        RowVisitor createRowVisitor(List<CellAtRow> cellsAtRows) {
            return new RowVisitor() {

                int cellIndex;
                Properties bindings;
                List<CellAtRow> cellsAtRows;

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
