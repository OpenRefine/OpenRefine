package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TypeConstraint implements Constraint {

    private String qid;
    private String relation;
    private String instanceOf;
    private String subclassOf;
    private String instanceOrSubclassOf;
    private String klass; // i.e. class

    @JsonCreator
    public TypeConstraint(@JsonProperty("qid") String qid,
                          @JsonProperty("relation") String relation,
                          @JsonProperty("instance_of") String instanceOf,
                          @JsonProperty("subclass_of") String subclassOf,
                          @JsonProperty("instance_of_or_subclass_of") String instanceOrSubclassOf,
                          @JsonProperty("class") String klass) {
        this.qid = qid;
        this.relation = relation;
        this.instanceOf = instanceOf;
        this.subclassOf = subclassOf;
        this.instanceOrSubclassOf = instanceOrSubclassOf;
        this.klass = klass;
    }

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
