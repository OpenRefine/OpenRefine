package org.openrefine.importers;

import java.io.Serializable;

import org.openrefine.model.GridState;
import org.openrefine.util.ParsingUtilities;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class LineBasedImporterTests extends ImporterTest {
	
	LineBasedImporter SUT;

	@BeforeMethod
	public void setUpImporter() {
		SUT = new LineBasedImporter(runner());
	}
	
	@AfterMethod
	public void tearDownImporter() {
		SUT = null;
	}
	
	@Test
	public void testLineBasedImporter() throws Exception {
		String contents = ""
				+ "foo\n"
				+ "bar\n"
				+ "baz";
		
		ObjectNode options = ParsingUtilities.mapper.createObjectNode();
		GridState parsed = parseOneString(SUT, contents, options);
		
		GridState expected = createGrid(new String[] { "Column 1" },
				new Serializable[][] {
			{ "foo" },
			{ "bar" },
			{ "baz" }
		});
		
		assertGridEquals(parsed, expected);
	}
}
