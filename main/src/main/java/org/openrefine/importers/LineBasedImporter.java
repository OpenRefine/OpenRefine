
package org.openrefine.importers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.RowMapper;

/**
 * An importer which reads each line of a file as a row with a single string-valued cell containing the line.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LineBasedImporter extends LineBasedImporterBase {

    public LineBasedImporter(DatamodelRunner runner) {
        super(runner);
    }

    @Override
    protected RowMapper getRowMapper(ObjectNode options) {
        return RowMapper.IDENTITY;
    }

    @Override
    protected int getColumnCount(GridState rawCells, RowMapper rowMapper, ObjectNode options) {
        return 1;
    }

}
