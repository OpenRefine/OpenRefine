package org.openrefine.wikidata.manifests.constraints;

public class AllowedEntityTypesConstraint implements Constraint {

    private String qid;
    private String itemOfPropertyConstraint;
    private String wikibaseItem;
    private String wikibaseProperty;
    private String lexeme;
    private String form;
    private String sense;
    private String wikibaseMediainfo;

    public String getQid() {
        return qid;
    }

    public String getItemOfPropertyConstraint() {
        return itemOfPropertyConstraint;
    }

    public String getWikibaseItem() {
        return wikibaseItem;
    }

    public String getWikibaseProperty() {
        return wikibaseProperty;
    }

    public String getLexeme() {
        return lexeme;
    }

    public String getForm() {
        return form;
    }

    public String getSense() {
        return sense;
    }

    public String getWikibaseMediainfo() {
        return wikibaseMediainfo;
    }
}
