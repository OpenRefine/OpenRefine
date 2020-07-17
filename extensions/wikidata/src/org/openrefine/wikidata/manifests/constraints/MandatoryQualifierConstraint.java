package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MandatoryQualifierConstraint implements Constraint {

    private String qid;
    private String property;

    @JsonCreator
    public MandatoryQualifierConstraint(@JsonProperty("qid") String qid,
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
