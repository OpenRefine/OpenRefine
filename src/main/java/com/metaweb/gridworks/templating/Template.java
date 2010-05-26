package com.metaweb.gridworks.templating;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

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
        final protected int 		limit;
        final protected Writer  	writer;
        protected Properties 		bindings;
        
        public int total;
        
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
    			//writer.write("[Error: " + ((EvalError) v).message);
        	} else if (v instanceof String) {
        		writer.write((String) v);
        	} else {
        		writer.write(v.toString());
        	}
        }
        
        public boolean internalVisit(Project project, int rowIndex, Row row) {
        	try {
	            if (total > 0 && _separator != null) {
	            	writer.write(_separator);
	            }
	            
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
