package com.metaweb.gridworks.history;

import java.io.Serializable;

import com.metaweb.gridworks.model.Project;

public interface Change extends Serializable {
	public void apply(Project project);
	public void revert(Project project);
}
