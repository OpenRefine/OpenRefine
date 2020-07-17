package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DifferenceWithinRangeConstraint implements Constraint {

    private String qid;
    private String property;
    private String minimumValue;
    private String maximumValue;

    @JsonCreator
    public DifferenceWithinRangeConstraint(@JsonProperty("qid") String qid,
                                           @JsonProperty("property") String property,
                                           @JsonProperty("minimum_value") String minimumValue,
                                           @JsonProperty("maximum") String maximumValue) {
        this.qid = qid;
        this.property = property;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }

    public String getQid() {
        return qid;
    }

    public String getProperty() {
        return property;
    }

    public String getMinimumValue() {
        return minimumValue;
    }

    public String getMaximumValue() {
        return maximumValue;
    }
}
