
package com.google.refine.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.Validate;

/**
 * A column added by an operation, part of a {@link ColumnsDiff}. <br>
 * Supplying the name of the column it is inserted after makes it possible to pre-compute the order of the columns in
 * the resulting grid, which is useful for visualization purposes.
 */
public class AddedColumn {

    private final String name;
    private final String afterName;

    /**
     * @param name
     *            the name of the column being added
     * @param afterName
     *            optionally, the name of the column to its left, specifying the insertion point of the column.
     *            Otherwise, null.
     */
    public AddedColumn(String name, String afterName) {
        Validate.notNull(name);
        this.name = name;
        this.afterName = afterName;
    }

    /**
     * The name of the column being added.
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of the column it is added after, or null if it is not known.
     */
    @JsonProperty("afterName")
    public String getAfterName() {
        return afterName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afterName, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AddedColumn other = (AddedColumn) obj;
        return Objects.equals(afterName, other.afterName) && Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return "AddedColumn [name=" + name + ", afterName=" + afterName + "]";
    }
}
