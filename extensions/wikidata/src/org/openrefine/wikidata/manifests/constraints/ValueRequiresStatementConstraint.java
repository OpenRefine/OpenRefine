package org.openrefine.wikidata.manifests.constraints;

public class ValueRequiresStatementConstraint implements Constraint {

    private String qid;
    private String property;
    private String itemOfPropertyConstraint;

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
