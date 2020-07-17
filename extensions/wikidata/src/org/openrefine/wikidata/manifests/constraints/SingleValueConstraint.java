package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingleValueConstraint implements Constraint {

    private String qid;
    private String separator;

    @JsonCreator
    public SingleValueConstraint(@JsonProperty("qid") String qid,
                                 @JsonProperty("separator") String separator) {
        this.qid = qid;
        this.separator = separator;
    }

    public String getQid() {
        return qid;
    }

    public String getSeparator() {
        return separator;
    }
}
