package com.google.refine.tests;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;

public class ProjectFake extends Project{

	@Override
	public ProjectMetadata getMetadata() {
		return new ProjectMetadata();
	}
}
