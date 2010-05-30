package com.metaweb.gridworks.model;

public class Record {
	final public int fromRowIndex;
	final public int toRowIndex;
	final public int recordIndex;
	
	public Record(
		int fromRowIndex,
		int toRowIndex,
		int recordIndex
	) {
		this.fromRowIndex = fromRowIndex;
		this.toRowIndex = toRowIndex;
		this.recordIndex = recordIndex;
	}
}
