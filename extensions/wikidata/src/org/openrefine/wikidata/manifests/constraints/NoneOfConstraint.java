package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NoneOfConstraint implements Constraint {

    private String qid;
    private String itemOfPropertyConstraint;

    @JsonCreator
    public NoneOfConstraint(@JsonProperty("qid") String qid,
                            @JsonProperty("item_of_property_constraint") String itemOfPropertyConstraint) {
        this.qid = qid;
        this.itemOfPropertyConstraint = itemOfPropertyConstraint;
    }

    public String getQid() {
        return qid;
    }

    public String getItemOfPropertyConstraint() {
        return itemOfPropertyConstraint;
    }
}
