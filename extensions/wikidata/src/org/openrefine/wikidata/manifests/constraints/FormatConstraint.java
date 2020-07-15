package org.openrefine.wikidata.manifests.constraints;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FormatConstraint implements Constraint {

    private String qid;
    // Jackson maps "formatAsARegularExpression" to "format_as_aregular_expression"
    // with the PropertyNamingStrategy.SNAKE_CASE strategy.
    // So we need to specify the json property here manually.
    @JsonProperty("format_as_a_regular_expression")
    private String formatAsARegularExpression;
    private String syntaxClarification;

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
