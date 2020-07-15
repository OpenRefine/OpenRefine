package org.openrefine.wikidata.manifests.constraints;

public class PropertyScopeConstraint implements Constraint {

    private String qid;
    private String propertyScope;
    private String asMainValue;
    private String asQualifiers;
    private String asReferences;

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
