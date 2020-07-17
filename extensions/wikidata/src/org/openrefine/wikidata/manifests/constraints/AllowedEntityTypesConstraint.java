package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AllowedEntityTypesConstraint implements Constraint {

    private String qid;
    private String itemOfPropertyConstraint;
    private String wikibaseItem;
    private String wikibaseProperty;
    private String lexeme;
    private String form;
    private String sense;
    private String wikibaseMediainfo;

    @JsonCreator
    public AllowedEntityTypesConstraint(@JsonProperty("qid") String qid,
                                        @JsonProperty("item_of_property_constraint") String itemOfPropertyConstraint,
                                        @JsonProperty("wikibase_item") String wikibaseItem,
                                        @JsonProperty("wikibase_property") String wikibaseProperty,
                                        @JsonProperty("lexeme") String lexeme,
                                        @JsonProperty("form") String form,
                                        @JsonProperty("sense") String sense,
                                        @JsonProperty("wikibase_mediainfo") String wikibaseMediainfo) {
        this.qid = qid;
        this.itemOfPropertyConstraint = itemOfPropertyConstraint;
        this.wikibaseItem = wikibaseItem;
        this.wikibaseProperty = wikibaseProperty;
        this.lexeme = lexeme;
        this.form = form;
        this.sense = sense;
        this.wikibaseMediainfo = wikibaseMediainfo;
    }

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
