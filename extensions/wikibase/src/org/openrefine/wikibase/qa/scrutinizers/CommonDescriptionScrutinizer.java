
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.LabeledStatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import java.util.Set;

/**
 * @author Lu Liu
 */
public class CommonDescriptionScrutinizer extends DescriptionScrutinizer {

    public static final String descTooLongType = "description-too-long";
    public static final String descIdenticalWithLabel = "description-identical-with-label";

    @Override
    public void scrutinize(LabeledStatementEntityEdit update, String descText, String lang) {
        checkLength(update, descText, lang);
        checkLabel(update, descText, lang);
    }

    // Descriptions are not full sentences, but small bits of information.
    // In most cases, the proper length is between two and twelve words.
    protected void checkLength(LabeledStatementEntityEdit update, String descText, String lang) {
        final int maxLength = 250;
        if (descText.length() > maxLength) {
            QAWarning issue = new QAWarning(descTooLongType, null, QAWarning.Severity.CRITICAL, 1);
            issue.setProperty("example_entity", update.getEntityId());
            issue.setProperty("description", descText);
            issue.setProperty("lang", lang);
            issue.setProperty("length", descText.length());
            issue.setProperty("max_length", maxLength);
            addIssue(issue);
        }
    }

    // Description are expected to be more specific than labels.
    protected void checkLabel(LabeledStatementEntityEdit update, String descText, String lang) {
        Set<MonolingualTextValue> labels = update.getLabels();
        labels.addAll(update.getLabelsIfNew()); // merge
        for (MonolingualTextValue label : labels) {
            String labelText = label.getText();
            if (labelText == null) {
                continue;
            }
            labelText = labelText.trim();
            if (labelText.equals(descText)) {
                QAWarning issue = new QAWarning(descIdenticalWithLabel, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_entity", update.getEntityId());
                issue.setProperty("description", descText);
                issue.setProperty("lang", lang);
                issue.setProperty("label_lang", label.getLanguageCode());
                addIssue(issue);
                break;
            }
        }
    }

    @Override
    public boolean prepareDependencies() {
        return true;
    }
}
