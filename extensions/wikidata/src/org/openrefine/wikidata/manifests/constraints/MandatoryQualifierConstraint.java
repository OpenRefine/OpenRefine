package org.openrefine.wikidata.manifests.constraints;

public class MandatoryQualifierConstraint implements Constraint {

    private String qid;
    private String property;

    public String getQid() {
        return qid;
    }

    public String getProperty() {
        return property;
    }
}
