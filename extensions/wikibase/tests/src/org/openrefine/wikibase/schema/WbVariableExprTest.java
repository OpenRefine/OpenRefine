
package org.openrefine.wikibase.schema;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;

public class WbVariableExprTest extends WbExpressionTest<StringValue> {

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), true);
        columnModel.addColumn(1, new Column(1, "column B"), true);
        columnModel.addColumn(2, new Column(2, "column C"), true);

        hasNoValidationError(new WbStringVariable("column A"), columnModel);
        hasValidationError("Column 'foo' does not exist", new WbStringVariable("foo"), columnModel);
    }

}
