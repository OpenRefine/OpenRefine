package com.metaweb.gridlock.model;

import java.util.ArrayList;
import java.util.List;

public class Row {
	public boolean		flagged;
	public boolean		starred;
	public List<Cell> 	cells;
	
	public Row(int cellCount) {
		cells = new ArrayList<Cell>();
	}
}
