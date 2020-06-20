package org.openrefine.model.changes;

import java.io.Serializable;

import org.openrefine.RefineTest;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TransposeRowsIntoColumnsChangeTests extends RefineTest {
	
	GridState initial;
	
	@BeforeTest
	public void setUpGrid() {
		initial = createGrid(
				new String[] { "a", "b", "c" },
				new Serializable[][] {
			{ "1", "2", "3" },
			{ "4", "5", "6" },
			{ "7", "8", "9" },
			{ "10", "11", "12" }
		});
	}
	
	@Test(expectedExceptions = DoesNotApplyException.class)
	public void testDoesNotApply() throws DoesNotApplyException {
		(new TransposeRowsIntoColumnsChange("d", 2)).apply(initial);
	}
	
	@Test
	public void testTransposeRowsIntoColumns() throws DoesNotApplyException {
		Change change = new TransposeRowsIntoColumnsChange("b", 2);
		
		GridState expected = createGrid(
				new String[] { "a", "b 1", "b 2", "c" },
				new Serializable[][] {
			{ "1",  "2",  "5",  "3" },
			{ "4",  null, null, "6" },
			{ "7",  "8",  "11", "9" },
			{ "10", null, null, "12" }
		});
		
		assertGridEquals(change.apply(initial), expected);
	}
	
	@Test
	public void testTransposeRecordsIntoRows() throws DoesNotApplyException {
		GridState initialRecords = createGrid(
				new String[] { "a", "b", "c" },
				new Serializable[][] {
			{ "1",  "2",  "3"  },
			{ null, "5",  null },
			{ "7",  "8",  "9"  },
			{ null, "11", null }
		});
		
		Change change = new TransposeRowsIntoColumnsChange("b", 2);
		
		GridState expected = createGrid(
				new String[] { "a", "b 1", "b 2", "c" },
				new Serializable[][] {
			{ "1",  "2",  "5",  "3" },
			{ "7",  "8",  "11", "9" }
		});
		
		assertGridEquals(change.apply(initialRecords), expected);
	}
}
