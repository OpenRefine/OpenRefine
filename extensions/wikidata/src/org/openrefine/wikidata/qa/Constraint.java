package org.openrefine.wikidata.qa;

import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.List;
import java.util.Set;

public class Constraint {

    public static String CONSTRAINT_STATUS = "P2316";
    public static String CONSTRAINT_EXCEPTIONS = "P2303";

    Value constraintStatus;
    Set<Value> constraintExceptions;

    Constraint(Statement statement) {
        List<SnakGroup> snakGroupList = statement.getClaim().getQualifiers();
        for(SnakGroup group : snakGroupList) {
            for (Snak snak : group.getSnaks()) {
                if (group.getProperty().getId().equals(CONSTRAINT_STATUS)) {
                    constraintStatus = snak.getValue();
                }
                else if (group.getProperty().getId().equals(CONSTRAINT_EXCEPTIONS)) {
                    constraintExceptions.add(snak.getValue());
                }
            }
        }
    }

    public Value getConstraintStatus(Statement statement) {
        return this.constraintStatus;
    }

    public Set<Value> getConstraintExceptions(Statement statement) {
        return this.constraintExceptions;
    }
}
