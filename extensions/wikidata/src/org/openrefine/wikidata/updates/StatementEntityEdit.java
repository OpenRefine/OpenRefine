package org.openrefine.wikidata.updates;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A candidate edit on an entity which can bear statements.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface StatementEntityEdit extends EntityEdit {

	/**
	 * Groups added statements in {@link StatementGroupsEdit} objects.
	 */
	@JsonProperty("statementGroups")
	List<StatementGroupEdit> getStatementGroupEdits();
	
}
