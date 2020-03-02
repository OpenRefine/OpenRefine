package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import java.util.Set;

/**
 * A scrutinizer that checks the description of an item.
 * <p>
 * This abstract scrutinizer does the following checks:
 * 1. is the description too long
 * 2. is the description identical with the label in the same language
 * <p>
 * We can easily implement a language-specific description scrutinizer
 * by extending this class.
 *
 * @author Lu Liu
 */
public abstract class DescriptionScrutinizer extends EditScrutinizer {

    public static final String descTooLongType = "item-description-too-long";
    public static final String descIdenticalWithLabel = "item-description-identical-with-label";

    @Override
    public void scrutinize(ItemUpdate update) {
        Set<MonolingualTextValue> descriptions = update.getDescriptions();
        descriptions.addAll(update.getDescriptionsIfNew()); // merge
        for (MonolingualTextValue description : descriptions) {
            String descText = description.getText();
            if (descText == null) continue;
            descText = descText.trim();
            if (descText.length() == 0) continue; // avoid NullPointerException

            String lang = description.getLanguageCode();

            checkLength(update, descText, lang);
            checkLabel(update, descText, lang);

            scrutinize(update, descText, lang);
        }
    }

    public abstract void scrutinize(ItemUpdate update, String descText, String lang);

    // Descriptions are not full sentences, but small bits of information.
    // In most cases, the proper length is between two and twelve words.
    protected void checkLength(ItemUpdate update, String descText, String lang) {
        final int maxLength = 250;
        if (descText.length() > maxLength) {
            QAWarning issue = new QAWarning(descTooLongType, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", update.getItemId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", lang);
            issue.setProperty("length", descText.length());
            issue.setProperty("max_length", maxLength);
            addIssue(issue);
        }
    }

    // Description are expected to be more specific than labels.
    protected void checkLabel(ItemUpdate update, String descText, String lang) {
        Set<MonolingualTextValue> labels = update.getLabels();
        labels.addAll(update.getLabelsIfNew()); // merge
        for (MonolingualTextValue label : labels) {
            String labelText = label.getText();
            if (labelText == null) continue;
            labelText = labelText.trim();
            if (labelText.equals(descText)) {
                QAWarning issue = new QAWarning(descIdenticalWithLabel, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_entity", update.getItemId());
                issue.setProperty("description", descText);
                issue.setProperty("lang", lang);
                issue.setProperty("label_lang", label.getLanguageCode());
                addIssue(issue);
                break;
            }
        }
    }

}
