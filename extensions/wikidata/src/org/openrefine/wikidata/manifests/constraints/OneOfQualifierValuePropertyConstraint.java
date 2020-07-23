package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OneOfQualifierValuePropertyConstraint implements Constraint {

    private String qid;
    private String property;
    private String itemOfPropertyConstraint;

    @JsonCreator
    public OneOfQualifierValuePropertyConstraint(@JsonProperty("qid") String qid,
                                                 @JsonProperty("property") String property,
                                                 @JsonProperty("item_of_property_constraint") String itemOfPropertyConstraint) {
        this.qid = qid;
        this.property = property;
        this.itemOfPropertyConstraint = itemOfPropertyConstraint;
    }

    @Override
    public String getQid() {
        return qid;
    }

    public String getProperty() {
        return property;
    }

    public String getItemOfPropertyConstraint() {
        return itemOfPropertyConstraint;
    }
}
