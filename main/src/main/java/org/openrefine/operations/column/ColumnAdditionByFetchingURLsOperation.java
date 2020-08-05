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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.WrappedCell;
import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.changes.CellAtRow;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.ColumnAdditionByChangeData;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OnError;
import org.openrefine.process.LongRunningProcess;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


public class ColumnAdditionByFetchingURLsOperation extends EngineDependentOperation {
	final static private String urlChangeDataId = "urls";
	
    public static final class HttpHeader implements Serializable {

		private static final long serialVersionUID = -7546898562925960196L;
		@JsonProperty("name")
        final public String name;
        @JsonProperty("value")
        final public String value;
        
        @JsonCreator
        public HttpHeader(
                @JsonProperty("name")
                String name,
                @JsonProperty("value")
                String value) {
            this.name = name;
            this.value = value;
        }
    }
    
    final protected String     _baseColumnName;
    final protected String     _urlExpression;
    final protected OnError    _onError;

    final protected String     _newColumnName;
    final protected int        _columnInsertIndex;
    final protected int        _delay;
    final protected boolean    _cacheResponses;
    final protected List<HttpHeader>  _httpHeadersJson;

    @JsonCreator
    public ColumnAdditionByFetchingURLsOperation(
        @JsonProperty("engineConfig")
        EngineConfig   engineConfig,
        @JsonProperty("baseColumnName")
        String         baseColumnName,
        @JsonProperty("urlExpression")
        String         urlExpression,
        @JsonProperty("onError")
        OnError        onError,
        @JsonProperty("newColumnName")
        String         newColumnName,
        @JsonProperty("columnInsertIndex")
        int            columnInsertIndex,
        @JsonProperty("delay")
        int            delay,
        @JsonProperty("cacheResponses")
        boolean        cacheResponses,
        @JsonProperty("httpHeadersJson")
        List<HttpHeader>      httpHeadersJson
    ) {
        super(engineConfig);

        _baseColumnName = baseColumnName;
        _urlExpression = urlExpression;
        _onError = onError;

        _newColumnName = newColumnName;
        _columnInsertIndex = columnInsertIndex;

        _delay = delay;
        _cacheResponses = cacheResponses;
        _httpHeadersJson = httpHeadersJson;
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
	public String getDescription() {
        return "Create column " + _newColumnName +
            " at index " + _columnInsertIndex +
            " by fetching URLs based on column " + _baseColumnName +
            " using expression " + _urlExpression;
    }

    protected String createDescription(ColumnMetadata column, List<CellAtRow> cellsAtRows) {
        return "Create new column " + _newColumnName +
            ", filling " + cellsAtRows.size() +
            " rows by fetching URLs based on column " + column.getName() +
            " and formulated as " + _urlExpression;
    }


    @Override
    public Process createProcess(History history, ProcessManager manager) throws Exception {
        Engine engine = createEngine(history.getCurrentGridState());

        Evaluable eval = MetaParser.parse(_urlExpression);

        return new ColumnAdditionByFetchingURLsProcess(
            history,
            manager,
            engine,
            eval,
            getDescription(),
            _cacheResponses
        );
    }
    
    protected static class URLFetchingChangeProducer implements RowChangeDataProducer<Cell> {
    	
		private static final long serialVersionUID = 131571544240263338L;
		final protected OnError    _onError;
    	final protected List<HttpHeader>  _httpHeaders;
    	final protected boolean     _cacheResponses;
    	final protected int         _delay;
    	final protected int         _cellIndex;
    	final protected String      _baseColumnName;
    	final protected Evaluable   _eval;
    	// initialized lazily for serializability
    	transient private LoadingCache<String, Serializable> _urlCache;
    	
    	protected URLFetchingChangeProducer(
    			OnError onError,
    			List<HttpHeader> httpHeaders,
    			boolean cacheResponses,
    			int delay,
    			int cellIndex,
    			String baseColumnName,
    			Evaluable eval) {
    		_onError = onError;
    		_httpHeaders = httpHeaders;
    		_cacheResponses = cacheResponses;
    		_delay = delay;
    		_cellIndex = cellIndex;
    		_baseColumnName = baseColumnName;
    		_eval = eval;
    	}
    	
		@Override
		public Cell call(long rowId, Row row) {
			
			Cell cell = row.getCell(_cellIndex);
            Cell urlCell = null;

            Properties bindings = new Properties();
            ExpressionUtils.bind(bindings, null, row, rowId, _baseColumnName, cell);

            Object o = _eval.evaluate(bindings);
            if (o != null) {
                if (o instanceof Cell) {
                    urlCell = (Cell) o;
                } else if (o instanceof WrappedCell) {
                    urlCell = ((WrappedCell) o).cell;
                } else {
                    Serializable v = ExpressionUtils.wrapStorable(o);
                    if (ExpressionUtils.isNonBlankData(v)) {
                        urlCell = new Cell(v.toString(), null);
                    }
                }
            }
            String urlString = urlCell.value.toString();
            Serializable response = null;
            if (_cacheResponses) {
                response = cachedFetch(urlString);
            } else {
                response = fetch(urlString);
            }

            if (response != null) {
                return new Cell(response, null);
            }
			return null;
		}
		
		protected LoadingCache<String, Serializable> getCache() {
			if (_urlCache != null) {
				return _urlCache;
			}
			_urlCache = CacheBuilder.newBuilder()
	                .maximumSize(2048)
	                .expireAfterWrite(10, TimeUnit.MINUTES)
	                .build(
	                     new CacheLoader<String, Serializable>() {
	                        public Serializable load(String urlString) throws Exception {
	                            Serializable result = fetch(urlString);
	                            try {
	                                // Always sleep for the delay, no matter how long the
	                                // request took. This is more responsible than subtracting
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
			return _urlCache;
		}
		
        Serializable cachedFetch(String urlString) {
            try {
                return getCache().get(urlString);
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
                if (_httpHeaders != null) {
                    for (int i = 0; i < _httpHeaders.size(); i++) {
                        String headerLabel = _httpHeaders.get(i).name;
                        String headerValue = _httpHeaders.get(i).value;
                        if (headerValue != null && !headerValue.isEmpty()) {
                            urlConnection.setRequestProperty(headerLabel, headerValue);
                        }
                    }
                }

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
    	
    }


    public class ColumnAdditionByFetchingURLsProcess extends LongRunningProcess implements Runnable {
        final protected History       _history;
        final protected Engine        _engine;
        final protected Evaluable     _eval;
        final protected long          _historyEntryID;
        final protected ProcessManager _processManager;
        protected int                 _cellIndex;
   

        public ColumnAdditionByFetchingURLsProcess(
            History history,
            ProcessManager processManager,
            Engine engine,
            Evaluable eval,
            String description,
            boolean cacheResponses
        ) {
            super(description);
            _processManager = processManager;
            _history = history;
            _engine = engine;
            _eval = eval;
            _historyEntryID = HistoryEntry.allocateID();
        }
        
        @Override
        protected Runnable getRunnable() {
            return this;
        }

        @Override
        public void run() {
        	GridState state = _history.getCurrentGridState();
            ColumnMetadata column = state.getColumnModel().getColumnByName(_baseColumnName);
            if (column == null) {
                _processManager.onFailedProcess(this, new Exception("No column named " + _baseColumnName));
                return;
            }
            if (state.getColumnModel().getColumnByName(_newColumnName) != null) {
                _processManager.onFailedProcess(this, new Exception("Another column already named " + _newColumnName));
                return;
            }

            URLFetchingChangeProducer changeProducer = new URLFetchingChangeProducer(
            		_onError,
            		_httpHeadersJson,
            		_cacheResponses,
            		_delay,
            		_cellIndex,
            		_baseColumnName,
            		_eval);
            
            ChangeData<Cell> changeData = state.mapRows(_engine.combinedRowFilters(), changeProducer);
            
            try {
	            _history.getChangeDataStore().store(changeData, _historyEntryID, urlChangeDataId, new ColumnAdditionByChangeData.CellChangeDataSerializer());
	
	            if (!_canceled) {
	                HistoryEntry historyEntry = new HistoryEntry(
	                    _historyEntryID,
	                    _description,
	                    ColumnAdditionByFetchingURLsOperation.this,
	                    new ColumnAdditionByChangeData(
	                        urlChangeDataId,
	                        _columnInsertIndex,
	                        _newColumnName)
	                );
	
	                _history.addEntry(historyEntry);
	                _processManager.onDoneProcess(this);
	            }
            } catch(Exception e) {
            	_processManager.onFailedProcess(this, e);
            }
        }
    }

}
