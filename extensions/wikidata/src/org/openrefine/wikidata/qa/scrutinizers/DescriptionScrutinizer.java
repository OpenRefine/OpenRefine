package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import java.util.Set;

/**
 * @author Lu Liu
 */
public abstract class DescriptionScrutinizer extends EditScrutinizer {

    @Override
    public void scrutinize(ItemUpdate update) {
        Set<MonolingualTextValue> descriptions = update.getDescriptions();
        descriptions.addAll(update.getDescriptionsIfNew()); // merge
        for (MonolingualTextValue description : descriptions) {
            String descText = description.getText();
            if (descText == null) continue;
            descText = descText.trim();
            if (descText.length() == 0) continue; // avoid NullPointerException

            scrutinize(update, descText, description.getLanguageCode());
        }
    }

    public abstract void scrutinize(ItemUpdate update, String descText, String lang);

}
