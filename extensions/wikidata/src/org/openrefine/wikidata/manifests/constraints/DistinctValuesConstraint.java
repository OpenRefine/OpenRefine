package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DistinctValuesConstraint implements Constraint {

    private String qid;

    @JsonCreator
    public DistinctValuesConstraint(@JsonProperty("qid") String qid) {
        this.qid = qid;
    }

    public String getQid() {
        return qid;
    }
}
