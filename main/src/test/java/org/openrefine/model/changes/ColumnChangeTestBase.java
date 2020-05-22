package org.openrefine.model.changes;

import java.io.Serializable;

import org.openrefine.RefineTest;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.testng.annotations.BeforeTest;

public class ColumnChangeTestBase extends RefineTest {
	protected GridState initialState;
	
	@BeforeTest
	public void setUpInitialState() {
		MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
		Project project = createProject("my project", new String[] {"foo","bar","hello"},
				new Serializable[][] {
			{ "v1", "a", "d" },
			{ "v3", "a", "f" },
			{ "", "a", "g" },
			{ "", "b", "h" },
			{ new EvalError("error"), "a", "i"},
			{ "v1", "b", "j" }
		});
		initialState = project.getCurrentGridState();
	}
	
}
