
package org.openrefine.history;

import java.util.Collections;
import java.util.List;

import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;

public class OperationStubWithFacets implements Operation {

    @Override
    public Change.ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
        return null;
    }

    @Override
    public List<FacetConfig> getCreatedFacets() {
        return Collections.singletonList(new HistoryEntryTests.MyFacetConfig());
    }

    @Override
    public Change createChange() throws ParsingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        return "operation stub";
    }
}
