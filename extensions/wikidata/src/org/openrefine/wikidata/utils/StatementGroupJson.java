package org.openrefine.wikidata.utils;

import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wikidata-Toolkit's StatementGroup class is not designed to be serialized,
 * so its serialization via Jackson is not specified. This adds annotations
 * to specify its behaviour.
 * 
 * @author Antonin Delpeuch
 */
public class StatementGroupJson {
	
	protected final StatementGroup statementGroup;

	public StatementGroupJson(StatementGroup s) {
		statementGroup = s;
	}
	
	@JsonProperty("subject")
	public EntityIdValue getSubject() {
		return statementGroup.getSubject();
	}
	
	@JsonProperty("property")
	public PropertyIdValue getProperty() {
		return statementGroup.getProperty();
	}
	
	@JsonProperty("statements")
	public List<Statement> getStatements() {
		return statementGroup.getStatements();
	}

}
