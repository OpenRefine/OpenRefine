package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Project implements Serializable {
	private static final long serialVersionUID = -5089046824819472570L;
	
	public long id;
	public ColumnModel columnModel = new ColumnModel();
	public List<Row> rows = new ArrayList<Row>();
	
	public Project() {
		id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
	}
}
