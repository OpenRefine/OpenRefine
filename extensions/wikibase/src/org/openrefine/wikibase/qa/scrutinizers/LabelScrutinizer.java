
package org.openrefine.wikibase.qa.scrutinizers;

import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;

/**
 * @author Sebastian Berlin
 */
public class LabelScrutinizer extends EditScrutinizer {

    public static final String labelTooLongType = "label-too-long";
    private final int maxLength = 250;

    @Override
    public void scrutinize(ItemEdit update) {
    }

    @Override
    public void scrutinize(MediaInfoEdit update) {
        Set<MonolingualTextValue> labels = update.getLabels();
        labels.addAll(update.getLabelsIfNew());
        for (MonolingualTextValue label : labels) {
            String labelText = label.getText();
            if (labelText.length() > maxLength) {
                QAWarning issue = new QAWarning(labelTooLongType, null, QAWarning.Severity.CRITICAL, 1);
                issue.setProperty("example_entity", update.getEntityId());
                issue.setProperty("lang_code", label.getLanguageCode());
                issue.setProperty("length", labelText.length());
                issue.setProperty("max_length", maxLength);
                addIssue(issue);
            }
        }
    }

    @Override
    public boolean prepareDependencies() {
        return true;
    }
}
