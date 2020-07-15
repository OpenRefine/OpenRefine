package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueTypeConstraint implements Constraint {

    private String qid;
    private String relation;
    private String instanceOf;
    private String subclassOf;
    private String instanceOrSubclassOf;
    @JsonProperty("class")
    private String klass; // i.e. class

    public String getQid() {
        return qid;
    }

    public String getRelation() {
        return relation;
    }

    public String getInstanceOf() {
        return instanceOf;
    }

    public String getSubclassOf() {
        return subclassOf;
    }

    public String getInstanceOrSubclassOf() {
        return instanceOrSubclassOf;
    }

    public String getKlass() {
        return klass;
    }

}
