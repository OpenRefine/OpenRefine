
package org.openrefine.wikibase.qa;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Constraint class is defined to parse the common parameters of property constraints
 *
 * Most of the scrutinizer will have inner class defining the structure of that constraint will extend this Constraint
 * class
 *
 * @author Ekta Mishra
 */
public class Constraint {

    public static String CONSTRAINT_STATUS = "P2316";
    public static String CONSTRAINT_EXCEPTIONS = "P2303";

    ItemIdValue constraintStatus;
    Set<EntityIdValue> constraintExceptions;

    public Constraint(Statement statement) {
        constraintExceptions = new HashSet<>();
        List<SnakGroup> snakGroupList = statement.getClaim().getQualifiers();
        for (SnakGroup group : snakGroupList) {
            for (Snak snak : group.getSnaks()) {
                if (group.getProperty().getId().equals(CONSTRAINT_STATUS) && snak instanceof ValueSnak) {
                    constraintStatus = (ItemIdValue) ((ValueSnak) snak).getValue();
                } else if (group.getProperty().getId().equals(CONSTRAINT_EXCEPTIONS) && snak instanceof ValueSnak) {
                    constraintExceptions.add((EntityIdValue) ((ValueSnak) snak).getValue());
                }
            }
        }
    }

    public ItemIdValue getConstraintStatus() {
        return this.constraintStatus;
    }

    public Set<EntityIdValue> getConstraintExceptions() {
        return this.constraintExceptions;
    }
}
