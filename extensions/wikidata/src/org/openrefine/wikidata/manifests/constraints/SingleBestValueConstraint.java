package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingleBestValueConstraint implements Constraint {

    private String qid;
    private String separator;

    @JsonCreator
    public SingleBestValueConstraint(@JsonProperty("qid") String qid,
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
