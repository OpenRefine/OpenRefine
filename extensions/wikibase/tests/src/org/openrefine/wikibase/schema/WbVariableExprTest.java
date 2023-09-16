
package org.openrefine.wikibase.schema;

import java.util.Arrays;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.ModelException;

public class WbVariableExprTest extends WbExpressionTest<StringValue> {

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel(Arrays.asList(
                new ColumnMetadata("column A"),
                new ColumnMetadata("column B"),
                new ColumnMetadata("column C")));

        hasNoValidationError(new WbStringVariable("column A"), columnModel);
        hasValidationError("Column 'foo' does not exist", new WbStringVariable("foo"), columnModel);
    }

}
