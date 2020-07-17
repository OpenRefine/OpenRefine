package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContemporaryConstraint implements Constraint {

    private String qid;

    @JsonCreator
    public ContemporaryConstraint(@JsonProperty("qid") String qid) {
        this.qid = qid;
    }

    public String getQid() {
        return qid;
    }
}
