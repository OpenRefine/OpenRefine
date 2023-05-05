
package org.openrefine.history;

import java.util.Collections;
import java.util.List;

import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;

public class ChangeStubWithFacets implements Change {

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
        return null;
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

    @Override
    public List<FacetConfig> getCreatedFacets() {
        return Collections.singletonList(new HistoryEntryTests.MyFacetConfig());
    }
}
