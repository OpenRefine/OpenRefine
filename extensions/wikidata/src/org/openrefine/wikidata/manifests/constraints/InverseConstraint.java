package org.openrefine.wikidata.manifests.constraints;

public class InverseConstraint implements Constraint {

    private String qid;
    private String property;

    public String getQid() {
        return qid;
    }

    public String getProperty() {
        return property;
    }
}
