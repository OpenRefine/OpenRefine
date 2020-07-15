package org.openrefine.wikidata.manifests.constraints;

public class SingleValueConstraint implements Constraint {

    private String qid;
    private String separator;

    public String getQid() {
        return qid;
    }

    public String getSeparator() {
        return separator;
    }
}
