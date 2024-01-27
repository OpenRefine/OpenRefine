
package org.openrefine.model;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;

import org.openrefine.model.recon.ReconConfig;

/**
 * Represents a column written to by a row- or record-wise operation. This holds information about where the column
 * should be inserted, whether it is obtained by copying another column and how the new column should be named. <br>
 * This class is also able to represent overwriting columns, by marking the original column as deleted by the operation.
 * The {@link #getInsertAt()} field is then set to the original column name.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ColumnInsertion implements Serializable {

    private static final long serialVersionUID = 2912933886003703674L;
    private final String name;
    private final String insertAt;
    private final boolean replace;
    private final String copiedFrom;
    private final ReconConfig reconConfig;
    private final boolean overrideReconConfig;

    @JsonCreator
    public ColumnInsertion(
            @JsonProperty("name") String name,
            @JsonProperty("insertAt") String insertAt,
            @JsonProperty("replace") boolean replace,
            @JsonProperty("copiedFrom") String copiedFrom,
            @JsonProperty("reconConfig") ReconConfig reconConfig,
            @JsonProperty("overrideReconConfig") boolean overrideReconConfig) {
        Validate.notNull(name, "no name provided for column to insert");
        this.name = name;
        this.insertAt = insertAt;
        this.replace = replace;
        this.copiedFrom = copiedFrom;
        this.reconConfig = reconConfig;
        this.overrideReconConfig = overrideReconConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Short-hand form for creating a column insertion which just replaces an existing column by computing its new
     * values.
     * 
     * @param name
     *            the name of the column to replace
     */
    public static ColumnInsertion replacement(String name) {
        return builder()
                .withName(name)
                .withInsertAt(name)
                .withReplace(true)
                .build();
    }

    /**
     * The name of the inserted column after the operation was run.
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of the column after which this column should be inserted.
     * <ul>
     * <li>If this is null, the column is inserted at the beginning of the table</li>
     * <li>If this does not correspond to any previously existing column, then the column is inserted at the end of the
     * table</li>
     * </ul>
     */
    @JsonProperty("insertAt")
    @JsonInclude(Include.NON_NULL)
    public String getInsertAt() {
        return insertAt;
    }

    @JsonProperty("replace")
    public boolean isReplace() {
        return replace;
    }

    /**
     * The name of the column this column was copied from. This means that for all filtered rows/records, the value of
     * the inserted column in this row/record is identical to that from the original column. <br>
     * If there is no such column (meaning that the values stored in this new column are actually computed by the
     * operation) then this is set to null.
     */
    @JsonProperty("copiedFrom")
    @JsonInclude(Include.NON_NULL)
    public String getCopiedFrom() {
        return copiedFrom;
    }

    /**
     * The recon config to set on the column. If null, the recon config on the column will only be set to null if
     * {@link #getOverrideReconConfig()} is set to true.
     */
    @JsonIgnore
    public ReconConfig getReconConfig() {
        return reconConfig;
    }

    /**
     * Controls whether the value of {@link #getReconConfig()} should override any present {@link ReconConfig} on the
     * column or not.
     */
    @JsonIgnore
    public boolean getOverrideReconConfig() {
        return overrideReconConfig;
    }

    @Override
    public int hashCode() {
        return Objects.hash(copiedFrom, insertAt, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ColumnInsertion other = (ColumnInsertion) obj;
        return Objects.equals(copiedFrom, other.copiedFrom) && Objects.equals(insertAt, other.insertAt)
                && replace == other.replace && Objects.equals(name, other.name) && Objects.equals(reconConfig, other.reconConfig);
    }

    @Override
    public String toString() {
        return "ColumnInsertion [name=" + name + ", insertAt=" + insertAt +
                ", replace=" + replace + ", copiedFrom=" + copiedFrom + ", reconConfig=" + reconConfig + "]";
    }

    public static final class Builder {

        private String name = null;
        private String insertAt = null;
        private boolean replace = false;
        private String copiedFrom = null;
        private ReconConfig reconConfig = null;
        private boolean overrideReconConfig = false;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withInsertAt(String insertAt) {
            this.insertAt = insertAt;
            return this;
        }

        public Builder withReplace(boolean replace) {
            this.replace = replace;
            return this;
        }

        public Builder withCopiedFrom(String copiedFrom) {
            this.copiedFrom = copiedFrom;
            return this;
        }

        public Builder withReconConfig(ReconConfig reconConfig) {
            this.reconConfig = reconConfig;
            return this;
        }

        public Builder withOverrideReconConfig(boolean overrideReconConfig) {
            this.overrideReconConfig = overrideReconConfig;
            return this;
        }

        public ColumnInsertion build() {
            return new ColumnInsertion(
                    this.name, this.insertAt, this.replace,
                    this.copiedFrom, this.reconConfig, this.overrideReconConfig);
        }
    }

}
