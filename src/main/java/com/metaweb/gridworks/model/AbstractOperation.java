package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.Properties;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.process.Process;

/*
 *  An abstract operation can be applied to different but similar
 *  projects.
 */
public interface AbstractOperation extends Serializable, Jsonizable {
	public Process createProcess(Project project, Properties options) throws Exception;
}
