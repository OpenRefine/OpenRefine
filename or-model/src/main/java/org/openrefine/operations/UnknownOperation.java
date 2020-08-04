package org.openrefine.operations;

import java.util.HashMap;
import java.util.Map;

import org.openrefine.history.History;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An operation that is unknown to the current OpenRefine
 * instance, but might be interpretable by another instance
 * (for instance, a later version of OpenRefine, or using 
 * an extension).
 * 
 * This class holds the JSON serialization of the operation,
 * in the interest of being able to serialize it later, hence
 * avoiding to discard it and lose metadata.
 * 
 * @author Antonin Delpeuch
 *
 */
public class UnknownOperation implements Operation {
	
	// Map storing the JSON serialization of the operation in an agnostic way
	private Map<String, Object> properties;
	
	// Operation code and description stored separately
	private String opCode;
	private String description;
	
	@JsonCreator
	public UnknownOperation(
			@JsonProperty("op") String opCode,
			@JsonProperty("description") String description) {
		properties = new HashMap<>();
		this.opCode = opCode;
		this.description = description;
	}
	
	@JsonAnySetter
	public void setAttribute(String key, Object value) {
		properties.put(key, value);
	}
	
	@JsonAnyGetter
	public Map<String,Object> getAttributes() {
		return properties;
	}
	
    @JsonProperty("op")
    public String getOperationId() {
        return opCode;
    }
    
    public String getDescription() {
        return description;
    }

    @Override
    public Process createProcess(History history, ProcessManager manager) throws Exception {
        throw new IllegalStateException("An unknown operation cannot be executed");
    }
}
