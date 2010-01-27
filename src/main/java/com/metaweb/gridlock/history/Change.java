package com.metaweb.gridlock.history;

import java.io.Serializable;

import com.metaweb.gridlock.model.Project;

public interface Change extends Serializable {
	public void apply(Project project);
	public void revert(Project project);
}
