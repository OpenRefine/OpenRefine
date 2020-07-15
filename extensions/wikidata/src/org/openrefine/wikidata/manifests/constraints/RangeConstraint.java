package org.openrefine.wikidata.manifests.constraints;

public class RangeConstraint implements Constraint {

    private String qid;
    private String minimumValue;
    private String maximumValue;
    private String minimumDate;
    private String maximumDate;

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
