
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.LabeledStatementEntityEdit;

/**
 * @author Lu Liu
 */
public class EnglishDescriptionScrutinizer extends DescriptionScrutinizer {

    public static final String descEndsByPunctuationSign = "description-end-by-punctuation-sign";
    public static final String descBeginWithUppercase = "description-begin-with-uppercase";
    public static final String descBeginWithArticle = "description-begin-with-article";

    private static final String LANG = "en";

    @Override
    public void scrutinize(LabeledStatementEntityEdit update, String descText, String lang) {
        if (!LANG.equalsIgnoreCase(lang)) {
            return;
        }

        checkPunctuationSign(update, descText);
        checkUppercase(update, descText);
        checkArticle(update, descText);
    }

    // Description are not sentences, so the punctuation sign at the end should be avoided.
    protected void checkPunctuationSign(LabeledStatementEntityEdit update, String descText) {
        assert descText.length() > 0;
        final String punctuationSigns = ".?!;:,'\"";

        char last = descText.charAt(descText.length() - 1);
        if (punctuationSigns.indexOf(last) != -1) {
            QAWarning issue = new QAWarning(descEndsByPunctuationSign, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", update.getEntityId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", LANG);
            issue.setProperty("punctuation_sign", last);
            addIssue(issue);
        }
    }

    // Descriptions begin with a lowercase letter except when uppercase would normally be required or expected.
    protected void checkUppercase(LabeledStatementEntityEdit update, String descText) {
        assert descText.length() > 0;

        char first = descText.charAt(0);
        if ('A' <= first && first <= 'Z') {
            QAWarning issue = new QAWarning(descBeginWithUppercase, null, QAWarning.Severity.INFO, 1);
            issue.setProperty("example_entity", update.getEntityId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", LANG);
            issue.setProperty("uppercase_letter", first);
            addIssue(issue);
        }
    }

    // Descriptions should not normally begin with initial articles ("a", "an", "the").
    protected void checkArticle(LabeledStatementEntityEdit update, String descText) {
        assert descText.length() > 0;

        String firstWord = descText.split("\\s")[0].toLowerCase();
        if ("a".equals(firstWord) || "an".equals(firstWord) || "the".equals(firstWord)) {
            QAWarning issue = new QAWarning(descBeginWithArticle, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", update.getEntityId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", LANG);
            issue.setProperty("article", firstWord);
            addIssue(issue);
        }
    }

    @Override
    public boolean prepareDependencies() {
        return true;
    }
}
