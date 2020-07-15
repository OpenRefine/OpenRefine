package org.openrefine.wikidata.manifests.constraints;

public class DifferenceWithinRangeConstraint implements Constraint {

    private String qid;
    private String property;
    private String minimumValue;
    private String maximumValue;

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
