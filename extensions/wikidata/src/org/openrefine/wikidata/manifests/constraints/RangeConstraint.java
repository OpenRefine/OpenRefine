package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RangeConstraint implements Constraint {

    private String qid;
    private String minimumValue;
    private String maximumValue;
    private String minimumDate;
    private String maximumDate;

    @JsonCreator
    public RangeConstraint(@JsonProperty("qid") String qid,
                           @JsonProperty("minimum_value") String minimumValue,
                           @JsonProperty("maximum_value") String maximumValue,
                           @JsonProperty("minimum_date") String minimumDate,
                           @JsonProperty("maximum_date") String maximumDate) {
        this.qid = qid;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.minimumDate = minimumDate;
        this.maximumDate = maximumDate;
    }

    public String getQid() {
        return qid;
    }

    public String getMinimumValue() {
        return minimumValue;
    }

    public String getMaximumValue() {
        return maximumValue;
    }

    public String getMinimumDate() {
        return minimumDate;
    }

    public String getMaximumDate() {
        return maximumDate;
    }

}
