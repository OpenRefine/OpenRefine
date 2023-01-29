
package org.openrefine.history;

import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;

public class ChangeStub implements Change {

    @Override
    public Grid apply(Grid projectState, ChangeContext context) {
        return projectState;
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

}
