package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NoBoundsConstraint implements Constraint {

    private String qid;

    @JsonCreator
    public NoBoundsConstraint(@JsonProperty("qid") String qid) {
        this.qid = qid;
    }

    public String getQid() {
        return qid;
    }
}
