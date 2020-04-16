package com.google.refine.exporters;

import java.io.StringWriter;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
//import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
//import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.exporters.CustomizableTabularExporterUtilities.CellFormatter;
import com.google.refine.exporters.TabularSerializer.CellData;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.sun.org.slf4j.internal.LoggerFactory;
import com.sun.xml.internal.fastinfoset.sax.Properties;

@SuppressWarnings("restriction")
public class CustomizableTabularExporterUtilitiesTests extends RefineTest{
	
	private static final String TEST_PROJECT_NAME = "customizable tabular exporter utilites test project";
	
	@Mock
	private Column column;
	
	@Mock
	private Cell cell;
	
	@Override
	@BeforeTest
	public void init() {
		logger = (Logger) LoggerFactory.getLogger(this.getClass());
	}
	
	//dependencies
	StringWriter writer;
	ProjectMetadata projectMetadata;
	Project project;
	Engine engine;
	Properties options;
	
	//System Under Test
	//CustomizableTabularExporterUtilities SUT;
	CellFormatter SUT;
	
	@BeforeMethod
	public void SetUp(){
		SUT = new CustomizableTabularExporterUtilities.CellFormatter();
		writer = new StringWriter();
		ProjectManager.singleton = new ProjectManagerStub();
		projectMetadata = new ProjectMetadata();
		project = new Project();
		projectMetadata.setName(TEST_PROJECT_NAME);
		ProjectManager.singleton.registerProject(project, projectMetadata);
		engine =new Engine(project);
		options = mock(Properties.class);
	}
	
	@AfterMethod
	public void TearDown() {
		SUT = null;
		writer = null;
		ProjectManager.singleton.deleteProject(project.id);
		project = null;
		projectMetadata = null;
		engine = null;
		options = null;
	}
	
	@Test
	public void testNullFieldValue() {
		when(SUT.format(project, column, cell)).thenReturn(createCellDataWithNullValue());
		Assert.assertEquals(SUT.format(project, column, cell), createCellDataWithNullValue());
	}
	
	private CellData createCellDataWithNullValue() {
		return new CellData("testColumnName", null, "testText", "testLink");
	}
	
	@Test
	public void test() {
		when(SUT.format(project, column, cell)).thenReturn(createCellData());
		Assert.assertEquals(SUT.format(project, column, cell), createCellData());
	}
	
	private CellData createCellData() {
		return new CellData("testColumnName", "testValue", "testText", "testLink");
	}
	
}
