package org.openrefine.wikidata.manifests.constraints;

public class FormatConstraint implements Constraint {

    private String qid;
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
