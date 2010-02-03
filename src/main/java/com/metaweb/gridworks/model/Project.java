package com.metaweb.gridworks.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.metaweb.gridworks.history.History;
import com.metaweb.gridworks.process.ProcessManager;

public class Project implements Serializable {
	private static final long serialVersionUID = -5089046824819472570L;
	
	final public long 			id;
	
	final public ColumnModel 	columnModel = new ColumnModel();
	final public List<Row> 		rows = new ArrayList<Row>();
	final public History 		history;
	
	transient public ProcessManager processManager;
	
	public Project() {
		id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
		history = new History(this);
		
		internalInitialize();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		internalInitialize();
	}
	
	protected void internalInitialize() {
		processManager = new ProcessManager();
		
		computeContext();
	}
	
	protected void computeContext() {
		// TODO
	}
}
