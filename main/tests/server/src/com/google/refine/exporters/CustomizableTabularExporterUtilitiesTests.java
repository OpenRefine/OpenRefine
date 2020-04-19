package com.google.refine.exporters;

import static org.mockito.Mockito.mock;
import org.mockito.Mock;

import java.io.StringWriter;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


import org.slf4j.Logger;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.exporters.CustomizableTabularExporterUtilities.CellFormatter;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;


public class CustomizableTabularExporterUtilitiesTests extends RefineTest {

	private static final String TEST_PROJECT_NAME = "Customizable Tabular Exporter Utilites Test Project";
	private int cellIndex;
	private String columnName;
	
	@Mock
	private Column column = new Column(cellIndex,columnName);
	
	@Mock
	private Recon recon;
		
	
	private Cell cell = new Cell("testSerializableValue", recon);
	

	
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
	
	
	//test with null cellValue
		@Test
		public void testFormatWithNullFieldValue() {
					
			Assert.assertNotNull(SUT.format(project, column, null));
			
		}
		
		
	//test with notNull cellValue
		@Test
		public void testFormatWithNotNullFieldValue() {
			
			Assert.assertNotNull(SUT.format(project, column, cell));
		}
	

}
