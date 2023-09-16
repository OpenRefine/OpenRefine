
package org.openrefine.wikibase.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;

public class SnakUtils {

    /**
     * Groups snaks into a list of snak groups. The order of the first snaks in each group is preserved.
     * 
     * @param snaks
     * @return
     */
    public static List<SnakGroup> groupSnaks(List<Snak> snaks) {
        Map<PropertyIdValue, List<Snak>> snakGroups = new HashMap<>();
        List<PropertyIdValue> propertyOrder = new ArrayList<PropertyIdValue>();
        for (Snak snak : snaks) {
            List<Snak> existingSnaks = snakGroups.get(snak.getPropertyId());
            if (existingSnaks == null) {
                existingSnaks = new ArrayList<Snak>();
                snakGroups.put(snak.getPropertyId(), existingSnaks);
                propertyOrder.add(snak.getPropertyId());
            }
            if (!existingSnaks.contains(snak)) {
                existingSnaks.add(snak);
            }
        }
        return propertyOrder.stream()
                .map(pid -> Datamodel.makeSnakGroup(snakGroups.get(pid)))
                .collect(Collectors.toList());
    }

}
