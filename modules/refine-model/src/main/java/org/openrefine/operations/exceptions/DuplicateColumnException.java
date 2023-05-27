
package org.openrefine.operations.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DuplicateColumnException extends OperationException {

    private static final long serialVersionUID = -3979189003299948129L;
    private final String columnName;

    public DuplicateColumnException(String columnName) {
        super("duplicate_column", String.format("Column '%s' already exists in the project.", columnName));
        this.columnName = columnName;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }
}
