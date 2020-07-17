package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InverseConstraint implements Constraint {

    private String qid;
    private String property;

    @JsonCreator
    public InverseConstraint(@JsonProperty("qid") String qid,
                             @JsonProperty("property") String property) {
        this.qid = qid;
        this.property = property;
    }

    public String getQid() {
        return qid;
    }

    public String getProperty() {
        return property;
    }
}
