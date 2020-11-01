package org.openrefine.importers;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.model.GridState;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MarcImporterTests extends ImporterTest {
	
	MarcImporter parser;
	
	@BeforeMethod
	@Override
	public void setUp() {
		super.setUp();
		parser = new MarcImporter(runner());
		
	}
	
	@Test
	public void testParseSampleRecord() throws Exception {
		ObjectNode options = parser.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "collection");
        JSONUtilities.append(path, "record");
        JSONUtilities.safePut(options, "recordPath", path);
        
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("importers/sample.mrc");
		File copy = saveInputStreamToImporterTestDir(inputStream);
		
		List<ImportingFileRecord> importingFileRecords = Collections.singletonList(
				new ImportingFileRecord(null, copy.getName(), copy.getName(), 0, null, null, null, null, null, null, null, null));
		parser.createParserUIInitializationData(job, importingFileRecords, "marc");
		
		GridState grid = parseFiles(parser, importingFileRecords, options);
		
		List<String> columnNames = grid.getColumnModel().getColumnNames();
		Assert.assertTrue(columnNames.contains("record - datafield - tag"));
	}
	
}
