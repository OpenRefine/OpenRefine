package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertyScopeConstraint implements Constraint {

    private String qid;
    private String propertyScope;
    private String asMainValue;
    private String asQualifiers;
    private String asReferences;

    @JsonCreator
    public PropertyScopeConstraint(@JsonProperty("qid") String qid,
                                   @JsonProperty("property_scope") String propertyScope,
                                   @JsonProperty("as_main_value") String asMainValue,
                                   @JsonProperty("as_qualifiers") String asQualifiers,
                                   @JsonProperty("as_references") String asReferences) {
        this.qid = qid;
        this.propertyScope = propertyScope;
        this.asMainValue = asMainValue;
        this.asQualifiers = asQualifiers;
        this.asReferences = asReferences;
    }

    public String getQid() {
        return qid;
    }

    public String getPropertyScope() {
        return propertyScope;
    }

    public String getAsMainValue() {
        return asMainValue;
    }

    public String getAsQualifiers() {
        return asQualifiers;
    }

    public String getAsReferences() {
        return asReferences;
    }

}
