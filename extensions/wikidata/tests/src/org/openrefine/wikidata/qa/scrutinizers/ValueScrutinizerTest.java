package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public abstract class ValueScrutinizerTest extends SnakScrutinizerTest {
    
    public static final PropertyIdValue defaultPid = Datamodel.makeWikidataPropertyIdValue("P328");
    
    public void scrutinize(Value value) {
        scrutinize(Datamodel.makeValueSnak(defaultPid, value));
    }
    
    public void scrutinizeLabel(MonolingualTextValue text) {
        scrutinize(new ItemUpdateBuilder(TestingDataGenerator.existingId).addLabel(text).build());
    }
}
