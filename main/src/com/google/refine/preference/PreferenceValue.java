package com.google.refine.preference;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface to be extended by all objects stored 
 * in the preferences. This ensures that their full class
 * name is serialized with them. They should implement
 * Jackson deserialization as usual.
 * 
 * @author Antonin Delpeuch
 */

@JsonTypeInfo(
        use=JsonTypeInfo.Id.CLASS,
        include=JsonTypeInfo.As.PROPERTY,
        property="class")
public interface PreferenceValue {
	
	@JsonProperty("class")
	public default String getClassName() {
		return this.getClass().getName();
	}
}
