package org.openrefine.wikidata.updates;

import java.util.Objects;

import org.openrefine.wikidata.schema.strategies.StatementEditingMode;
import org.openrefine.wikidata.schema.strategies.StatementMerger;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An object which represents an edit on a statement,
 * which can be added or removed and have various merging strategies
 * with existing statements.
 * 
 * @author Antonin Delpeuch
 */
public class StatementEdit {
	
	protected final Statement statement;
	protected final StatementMerger merger;
	protected final StatementEditingMode mode;

	public StatementEdit(
			Statement statement,
			StatementMerger merger,
			StatementEditingMode mode) {
		this.statement = statement;
		this.merger = merger;
		this.mode = mode;
	}
	
	/**
	 * Constructs a statement update with a default merging strategy,
	 * useful for backwards compatibility (when no merging strategy could
	 * be specified).
	 * 
	 * @param statement
	 * @param mode
	 */
	public StatementEdit(
			Statement statement,
			StatementEditingMode mode) {
		this.statement = statement;
		this.merger = StatementMerger.FORMER_DEFAULT_STRATEGY;
		this.mode = mode;
	}
	
	@JsonProperty("statement")
	public Statement getStatement() {
		return statement;
	}
	
	@JsonProperty("mergingStrategy")
	public StatementMerger getMerger() {
		return merger;
	}
	
	@JsonProperty("mode")
	public StatementEditingMode getMode() {
		return mode;
	}
	
	/**
	 * Convenience method to directly access the property of the statement.
	 */
	@JsonIgnore
	public PropertyIdValue getPropertyId() {
		return statement.getMainSnak().getPropertyId();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(mode == StatementEditingMode.ADD_OR_MERGE ? "Add statement [" : "Remove statement [");
		builder.append(statement);
		builder.append(", ");
		builder.append(merger);
		builder.append("]");
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof StatementEdit)) {
			return false;
		}
		StatementEdit otherUpdate = (StatementEdit) other;
		return (statement.equals(otherUpdate.getStatement()) &&
				merger.equals(otherUpdate.getMerger()) &&
				mode.equals(otherUpdate.getMode()));
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(statement, merger, mode);
	}
}
