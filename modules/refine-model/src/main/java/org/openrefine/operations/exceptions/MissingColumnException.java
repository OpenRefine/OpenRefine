
package org.openrefine.operations.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exception thrown by an operation when the grid it is applied to misses a column.
 */
public class MissingColumnException extends OperationException {

    private static final long serialVersionUID = 1688102334703389780L;
    private final String columnName;

    public MissingColumnException(String columnName) {
        super("missing_column", String.format("Column '%s' does not exist.", columnName));
        this.columnName = columnName;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

}
