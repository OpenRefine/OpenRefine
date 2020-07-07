package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.ArrayList;
import java.util.List;

public class ItemRequiresScrutinizer extends EditScrutinizer {

    public static final String type = "property-should-have-certain-other-statement";
    public static String ITEM_REQUIRES_CONSTRAINT_QID = "Q21503247";
    public static String ITEM_REQUIRES_PROPERTY_PID = "P2306";
    public static String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";

    class ItemRequiresConstraint {
        final PropertyIdValue itemRequiresPid;
        final List<Value> itemList;

        ItemRequiresConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            PropertyIdValue pid = null;
            this.itemList = new ArrayList<>();
            for(SnakGroup group : specs) {
                for (Snak snak : group.getSnaks()) {
                    if (group.getProperty().getId().equals(ITEM_REQUIRES_PROPERTY_PID)){
                        pid = (PropertyIdValue) snak.getValue();
                    }
                    if (group.getProperty().getId().equals(ITEM_OF_PROPERTY_CONSTRAINT_PID)){
                        this.itemList.add(snak.getValue());
                    }
                }
            }
            this.itemRequiresPid = pid;
        }
    }

    @Override
    public void scrutinize(ItemUpdate edit) {

    }
}
