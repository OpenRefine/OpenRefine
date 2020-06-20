package org.openrefine.operations.cell;

import java.io.Serializable;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.NotImmediateOperationException;
import org.testng.annotations.Test;

public class TransposeColumnsIntoRowsTests extends RefineTest {
	
	/**
	 * This shows how the transpose columns into rows operation can, 
	 * in certain cases, be an inverse to the transpose rows into columns
	 * operation.
	 */
	@Test
	public void testTransposeBackToRecords() throws DoesNotApplyException, NotImmediateOperationException, ParsingException {
		GridState initialRecords = createGrid(
				new String[] { "a", "b 1", "b 2", "c" },
				new Serializable[][] {
			{ "1",  "2",  "5",  "3" },
			{ "7",  "8",  "11", "9" }
		});
		
		Operation op = new TransposeColumnsIntoRowsOperation(
		        "b 1", 2, true, false, "b", false, null
		    );
		Change change = op.createChange();
		
		GridState expected = createGrid(
				new String[] { "a", "b", "c" },
				new Serializable[][] {
			{ "1",  "2",  "3"  },
			{ null, "5",  null },
			{ "7",  "8",  "9"  },
			{ null, "11", null }
		});
		
		assertGridEquals(change.apply(initialRecords), expected);
	}
}
