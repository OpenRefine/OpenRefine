
package org.openrefine.history;

import java.util.Collections;
import java.util.List;

import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.DoesNotApplyException;

public class OperationStubWithFacets implements Operation {

    @Override
    public Operation.ChangeResult apply(Grid projectState, ChangeContext context) throws Operation.DoesNotApplyException {
        return null;
    }

    @Override
    public List<FacetConfig> getCreatedFacets() {
        return Collections.singletonList(new HistoryEntryTests.MyFacetConfig());
    }

    @Override
    public String getDescription() {
        return "operation stub";
    }
}
