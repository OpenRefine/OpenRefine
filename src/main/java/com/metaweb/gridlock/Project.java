package com.metaweb.gridlock;

import java.util.ArrayList;
import java.util.List;

import com.metaweb.gridlock.model.ColumnModel;
import com.metaweb.gridlock.model.Row;

public class Project {
	public long id;
	public ColumnModel columnModel = new ColumnModel();
	public List<Row> rows = new ArrayList<Row>();
	
	public Project() {
		id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
	}
}
