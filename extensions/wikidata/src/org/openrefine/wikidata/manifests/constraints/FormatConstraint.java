package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FormatConstraint implements Constraint {

    private String qid;
    private String formatAsARegularExpression;
    private String syntaxClarification;

    @JsonCreator
    public FormatConstraint(@JsonProperty("qid") String qid,
                            @JsonProperty("format_as_a_regular_expression") String formatAsARegularExpression,
                            @JsonProperty("syntax_clarification") String syntaxClarification) {
        this.qid = qid;
        this.formatAsARegularExpression = formatAsARegularExpression;
        this.syntaxClarification = syntaxClarification;
    }

    public String getQid() {
        return qid;
    }

    public String getFormatAsARegularExpression() {
        return formatAsARegularExpression;
    }

    public String getSyntaxClarification() {
        return syntaxClarification;
    }
}
