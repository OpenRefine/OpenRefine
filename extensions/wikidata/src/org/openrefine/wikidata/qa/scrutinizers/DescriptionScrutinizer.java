package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import java.util.Set;

/**
 * A scrutinizer that checks the description of an item.
 *
 * The checks work well for English.
 * It's impossible to cover all languages,
 * but since most edited information is in English,
 * merely focusing on English here should be enough.
 *
 * To be more specific, it does the following checks:
 * 1. is a description too long
 * 2. does a description end by punctuation signs
 * 3. does a description begin with a uppercase letter
 * 4. does a description begin with article ("a", "an" or "the")
 * 5. is the description identical with corresponding label
 *
 * @author Lu Liu
 */
public class DescriptionScrutinizer extends EditScrutinizer {

    public static final String descTooLongType = "item-description-too-long";
    public static final String descEndsByPunctuationSign = "item-description-end-by-punctuation-sign";
    public static final String descBeginWithUppercase = "item-description-begin-with-uppercase";
    public static final String descBeginWithArticle = "item-description-begin-with-article";
    public static final String descIdenticalWithLabel = "item-description-identical-with-label";

    private static final int descLengthThreshold = 250;

    private static final String punctuationSigns = ".!?,'\"";

    @Override
    public void scrutinize(ItemUpdate update) {
        Set<MonolingualTextValue> descriptions = update.getDescriptions();
        descriptions.addAll(update.getDescriptionsIfNew()); // merge
        for (MonolingualTextValue description : descriptions) {
            doScrutinize(update, description);
        }
    }

    private void doScrutinize(ItemUpdate update, MonolingualTextValue description) {
        String descText = description.getText();
        if (descText == null) return;
        descText = descText.trim();
        if (descText.length() == 0) return;

        // length check
        if (descText.length() > descLengthThreshold) {
            warningWithEntity(update, descTooLongType);
        }

        // punctuation sign check
        char last = descText.charAt(descText.length() - 1);
        if (punctuationSigns.indexOf(last) != -1) {
            warningWithEntity(update, descEndsByPunctuationSign);
        }

        // begin with uppercase letter check
        char first = descText.charAt(0);
        if ('A' <= first && first <= 'Z') {
            warningWithEntity(update, descBeginWithUppercase);
        }

        // article check
        String firstWord = descText.split("\\s")[0].toLowerCase();
        if ("a".equals(firstWord) || "an".equals(firstWord) || "the".equals(firstWord)) {
            warningWithEntity(update, descBeginWithArticle);
        }

        // description-label check
        Set<MonolingualTextValue> labels = update.getLabels();
        labels.addAll(update.getLabelsIfNew()); // merge
        for (MonolingualTextValue label : labels) {
            if (label.getLanguageCode().equals(description.getLanguageCode())) {
                String labelText = label.getText();
                if (labelText == null) break;
                labelText = labelText.trim();
                if (labelText.equals(descText)) {
                    warningWithEntity(update, descIdenticalWithLabel);
                }
                break;
            }
        }
    }

    private void warningWithEntity(ItemUpdate update, String type) {
        QAWarning issue = new QAWarning(type, null, QAWarning.Severity.WARNING, 1);
        issue.setProperty("example_entity", update.getItemId());
        addIssue(issue);
    }

}
