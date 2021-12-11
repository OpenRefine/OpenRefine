
package org.openrefine.history;

import org.openrefine.model.GridState;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;

public class ChangeStub implements Change {

    @Override
    public GridState apply(GridState projectState, ChangeContext context) {
        return projectState;
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

}
