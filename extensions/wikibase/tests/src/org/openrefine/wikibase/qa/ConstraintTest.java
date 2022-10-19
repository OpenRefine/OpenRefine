
package org.openrefine.wikibase.qa;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openrefine.wikibase.qa.Constraint.CONSTRAINT_EXCEPTIONS;
import static org.openrefine.wikibase.qa.Constraint.CONSTRAINT_STATUS;

public class ConstraintTest {

    public static final String SINGLE_VALUE_CONSTRAINT_QID = "Q19474404";

    public static ItemIdValue itemIdValue = Datamodel.makeWikidataItemIdValue(SINGLE_VALUE_CONSTRAINT_QID);
    public static PropertyIdValue constraintException = Datamodel.makeWikidataPropertyIdValue(CONSTRAINT_EXCEPTIONS);
    public static Value exceptionValue = Datamodel.makeWikidataItemIdValue("Q7409772");
    public static PropertyIdValue constraintStatus = Datamodel.makeWikidataPropertyIdValue(CONSTRAINT_STATUS);
    public static Value statusValue = Datamodel.makeWikidataItemIdValue("Q62026391");

    public static Constraint constraint;
    public static Statement statement;

    @BeforeMethod
    public void setUp() {
        Snak qualifierSnak1 = Datamodel.makeValueSnak(constraintException, exceptionValue);
        List<Snak> qualifierSnakList1 = Collections.singletonList(qualifierSnak1);
        Snak qualifierSnak2 = Datamodel.makeValueSnak(constraintStatus, statusValue);
        List<Snak> qualifierSnakList2 = Collections.singletonList(qualifierSnak2);
        SnakGroup qualifier1 = Datamodel.makeSnakGroup(qualifierSnakList1);
        SnakGroup qualifier2 = Datamodel.makeSnakGroup(qualifierSnakList2);
        List<SnakGroup> snakGroupList = Arrays.asList(qualifier1, qualifier2);
        PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, itemIdValue);
        Claim claim = Datamodel.makeClaim(itemIdValue, mainSnak, snakGroupList);
        Reference reference = Datamodel.makeReference(snakGroupList);
        List<Reference> referenceList = Collections.singletonList(reference);
        statement = Datamodel.makeStatement(claim, referenceList, StatementRank.NORMAL, "P2302$77BD7FE4-C051-4776-855C-543F0CE697D0");
        constraint = new Constraint(statement);
    }

    @Test
    public void testGetConstraintExceptions() {
        Set<EntityIdValue> entityIdValueSet = new HashSet<>();
        entityIdValueSet.add((EntityIdValue) exceptionValue);
        Assert.assertEquals(constraint.getConstraintExceptions(), entityIdValueSet);
    }

    @Test
    public void testGetConstraintStatus() {
        ItemIdValue status = (ItemIdValue) statusValue;
        Assert.assertEquals(constraint.getConstraintStatus(), status);
    }
}
