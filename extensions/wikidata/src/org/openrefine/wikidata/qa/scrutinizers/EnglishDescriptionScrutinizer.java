package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;

/**
 * @author Lu Liu
 */
public class EnglishDescriptionScrutinizer extends DescriptionScrutinizer {

    public static final String descEndsByPunctuationSign = "item-description-end-by-punctuation-sign";
    public static final String descBeginWithUppercase = "item-description-begin-with-uppercase";
    public static final String descBeginWithArticle = "item-description-begin-with-article";

    private static final String LANG = "en";

    @Override
    public void scrutinize(ItemUpdate update, String descText, String lang) {
        if (!LANG.equalsIgnoreCase(lang)) return;

        checkPunctuationSign(update, descText);
        checkUppercase(update, descText);
        checkArticle(update, descText);
    }

    // Description are not sentences, so the punctuation sign at the end should be avoided.
    protected void checkPunctuationSign(ItemUpdate update, String descText) {
        assert descText.length() > 0;
        final String punctuationSigns = ".?!;:,'\"";

        char last = descText.charAt(descText.length() - 1);
        if (punctuationSigns.indexOf(last) != -1) {
            QAWarning issue = new QAWarning(descEndsByPunctuationSign, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", update.getItemId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", LANG);
            issue.setProperty("punctuation_sign", last);
            addIssue(issue);
        }
    }

    // Descriptions begin with a lowercase letter except when uppercase would normally be required or expected.
    protected void checkUppercase(ItemUpdate update, String descText) {
        assert descText.length() > 0;

        char first = descText.charAt(0);
        if ('A' <= first && first <= 'Z') {
            QAWarning issue = new QAWarning(descBeginWithUppercase, null, QAWarning.Severity.INFO, 1);
            issue.setProperty("example_entity", update.getItemId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", LANG);
            issue.setProperty("uppercase_letter", first);
            addIssue(issue);
        }
    }

    // Descriptions should not normally begin with initial articles ("a", "an", "the").
    protected void checkArticle(ItemUpdate update, String descText) {
        assert descText.length() > 0;

        String firstWord = descText.split("\\s")[0].toLowerCase();
        if ("a".equals(firstWord) || "an".equals(firstWord) || "the".equals(firstWord)) {
            QAWarning issue = new QAWarning(descBeginWithArticle, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", update.getItemId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", LANG);
            issue.setProperty("article", firstWord);
            addIssue(issue);
        }
    }

}
