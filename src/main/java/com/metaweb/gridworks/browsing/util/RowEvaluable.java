package com.metaweb.gridworks.browsing.util;

import java.util.Properties;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public interface RowEvaluable {
	public Object eval(Project project, int rowIndex, Row row, Properties bindings);
}
