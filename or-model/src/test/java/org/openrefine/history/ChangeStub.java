
package org.openrefine.history;

import org.openrefine.model.GridState;

public class ChangeStub implements Change {

    @Override
    public GridState apply(GridState projectState) {
        return projectState;
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

}
