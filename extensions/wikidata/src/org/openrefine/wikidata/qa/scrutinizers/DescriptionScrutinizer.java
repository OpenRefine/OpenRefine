package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Set;

import org.openrefine.wikidata.updates.LabeledStatementEntityEdit;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

/**
 * @author Lu Liu
 */
public abstract class DescriptionScrutinizer extends EditScrutinizer {

    @Override
    public void scrutinize(TermedStatementEntityEdit update) {
        Set<MonolingualTextValue> descriptions = update.getDescriptions();
        descriptions.addAll(update.getDescriptionsIfNew()); // merge
        for (MonolingualTextValue description : descriptions) {
            String descText = description.getText();
            if (descText == null) {
                continue;
            }
            descText = descText.trim();
            if (descText.length() == 0) {
                continue; // avoid NullPointerException
            }

            scrutinize(update, descText, description.getLanguageCode());
        }
    }

    public abstract void scrutinize(LabeledStatementEntityEdit update, String descText, String lang);

}
