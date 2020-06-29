/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.qa;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MockConstraintFetcher implements ConstraintFetcher {

    public static PropertyIdValue pidWithInverse = Datamodel.makeWikidataPropertyIdValue("P350");
    public static PropertyIdValue inversePid = Datamodel.makeWikidataPropertyIdValue("P57");
    public static PropertyIdValue symmetricPid = Datamodel.makeWikidataPropertyIdValue("P783");
    public static PropertyIdValue allowedQualifierPid = Datamodel.makeWikidataPropertyIdValue("P34");
    public static PropertyIdValue mandatoryQualifierPid = Datamodel.makeWikidataPropertyIdValue("P97");

    public static PropertyIdValue mainSnakPid = Datamodel.makeWikidataPropertyIdValue("P1234");
    public static PropertyIdValue qualifierPid = Datamodel.makeWikidataPropertyIdValue("P987");
    public static PropertyIdValue referencePid = Datamodel.makeWikidataPropertyIdValue("P384");
    
    public static PropertyIdValue allowedValuesPid = Datamodel.makeWikidataPropertyIdValue("P8121");
    public static ItemIdValue allowedValueQid = Datamodel.makeWikidataItemIdValue("Q389");
    public static PropertyIdValue forbiddenValuesPid = Datamodel.makeWikidataPropertyIdValue("P8141");
    public static ItemIdValue forbiddenValueQid = Datamodel.makeWikidataItemIdValue("Q378");
    
    public static PropertyIdValue allowedUnitsPid = Datamodel.makeWikidataPropertyIdValue("P34787");
    public static ItemIdValue allowedUnit = Datamodel.makeWikidataItemIdValue("Q7887");
    public static PropertyIdValue noUnitsPid = Datamodel.makeWikidataPropertyIdValue("P334211");
    
    public static PropertyIdValue noBoundsPid = Datamodel.makeWikidataPropertyIdValue("P8932");
    public static PropertyIdValue integerPid = Datamodel.makeWikidataPropertyIdValue("P389");
    
    public static PropertyIdValue propertyOnlyPid = Datamodel.makeWikidataPropertyIdValue("P372");

    public static PropertyIdValue differenceWithinRangePid = Datamodel.makeWikidataPropertyIdValue("P570");
    public static PropertyIdValue lowerBoundPid = Datamodel.makeWikidataPropertyIdValue("P569");
    public static QuantityValue minValuePid = Datamodel.makeQuantityValue(new BigDecimal(0));
    public static QuantityValue maxValuePid = Datamodel.makeQuantityValue(new BigDecimal(150));

    public static PropertyIdValue conflictsWithPid = Datamodel.makeWikidataPropertyIdValue("P2002");
    public static Value conflictsWithStatementValue = Datamodel.makeWikidataItemIdValue("Q36322");
    public static PropertyIdValue conflictingStatement1Pid = Datamodel.makeWikidataPropertyIdValue("P31");
    public static Value conflictingStatement1Value = Datamodel.makeWikidataItemIdValue("Q4167836");
    public static PropertyIdValue conflictingStatement2Pid = Datamodel.makeWikidataPropertyIdValue("P553");
    public static Value conflictingStatement2Value = Datamodel.makeWikidataItemIdValue("Q918");

    @Override
    public String getFormatRegex(PropertyIdValue pid) {
        return "[1-9]\\d+";
    }

    /**
     * This constraint is purposely left inconsistent (the inverse constraint holds
     * only on one side).
     */
    @Override
    public PropertyIdValue getInversePid(PropertyIdValue pid) {
        if (pidWithInverse.equals(pid)) {
            return inversePid;
        }
        return null;
    }

    @Override
    public boolean allowedAsValue(PropertyIdValue pid) {
        return (!qualifierPid.equals(pid) && !referencePid.equals(pid));
    }

    @Override
    public boolean allowedAsQualifier(PropertyIdValue pid) {
        return (!mainSnakPid.equals(pid) && !referencePid.equals(pid));
    }

    @Override
    public boolean allowedAsReference(PropertyIdValue pid) {
        return (!mainSnakPid.equals(pid) && !qualifierPid.equals(pid));
    }

    @Override
    public Set<PropertyIdValue> allowedQualifiers(PropertyIdValue pid) {
        return Arrays.asList(allowedQualifierPid, mandatoryQualifierPid).stream().collect(Collectors.toSet());
    }

    @Override
    public Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue pid) {
        return Collections.singleton(mandatoryQualifierPid);
    }

    @Override
    public boolean hasSingleValue(PropertyIdValue pid) {
        return true;
    }
    
    @Override
    public boolean hasSingleBestValue(PropertyIdValue pid) {
        return false;
    }

    @Override
    public boolean hasDistinctValues(PropertyIdValue pid) {
        return true;
    }

    @Override
    public boolean hasMultiValue(PropertyIdValue pid) {
        return true;
    }

    @Override
    public boolean isSymmetric(PropertyIdValue pid) {
        return pid.equals(symmetricPid);
    }

    @Override
    public Set<Value> allowedValues(PropertyIdValue pid) {
        if (allowedValuesPid.equals(pid)) {
            return Arrays.asList(allowedValueQid, null).stream().collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Set<Value> disallowedValues(PropertyIdValue pid) {
        if (forbiddenValuesPid.equals(pid)) {
            return Collections.singleton(forbiddenValueQid);
        }
        return null;
    }

    @Override
    public boolean boundsAllowed(PropertyIdValue pid) {
        return !noBoundsPid.equals(pid);
    }

    @Override
    public boolean integerValued(PropertyIdValue pid) {
        return integerPid.equals(pid);
    }

    @Override
    public Set<ItemIdValue> allowedUnits(PropertyIdValue pid) {
        if(allowedUnitsPid.equals(pid)) {
            return Collections.singleton(allowedUnit);
        } else if(noUnitsPid.equals(pid)) {
            return Collections.singleton(null);
        }
        return null;
    }

    @Override
    public boolean usableOnItems(PropertyIdValue pid) {
        return !propertyOnlyPid.equals(pid);
    }

    @Override
    public QuantityValue getMinimumValue(PropertyIdValue pid) {
        if (differenceWithinRangePid.equals(pid)) {
            return minValuePid;
        }
        return null;
    }

    @Override
    public QuantityValue getMaximumValue(PropertyIdValue pid) {
        if (differenceWithinRangePid.equals(pid)) {
            return maxValuePid;
        }
        return null;
    }

    @Override
    public PropertyIdValue getLowerPropertyId(PropertyIdValue pid) {
        if (differenceWithinRangePid.equals(pid)){
            return lowerBoundPid;
        }
        return null;
    }

    @Override
    public boolean hasDiffWithinRange(PropertyIdValue pid) {
        return true;
    }

    @Override
    public List<Statement> getConstraintsByType(PropertyIdValue pid, String qid) {
        EntityIdValue entityIdValue = Datamodel.makeWikidataItemIdValue("Q21502838");
        PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
        Snak snak = Datamodel.makeValueSnak(propertyIdValue,entityIdValue);

        PropertyIdValue property = Datamodel.makeWikidataPropertyIdValue("P2306");
        Value propertyValue = Datamodel.makeWikidataPropertyIdValue("P31");
        Snak snak1 = Datamodel.makeValueSnak(property, propertyValue);
        List<Snak> group1 = Collections.singletonList(snak1);

        PropertyIdValue item = Datamodel.makeWikidataPropertyIdValue("P2305");
        Value itemValue = Datamodel.makeWikidataItemIdValue("Q5");
        Snak snak2 = Datamodel.makeValueSnak(item, itemValue);
        List<Snak> group2 = Collections.singletonList(snak2);

        SnakGroup snakGroup1 = Datamodel.makeSnakGroup(group1);
        SnakGroup snakGroup2 = Datamodel.makeSnakGroup(group2);

        List<SnakGroup> listSnakGroup = Arrays.asList(snakGroup1, snakGroup2);
        Claim claim = Datamodel.makeClaim(entityIdValue, snak, listSnakGroup);

        Reference reference = Datamodel.makeReference(listSnakGroup);
        List<Reference> referenceList = Collections.singletonList(reference);

        Statement statement = Datamodel.makeStatement(claim, referenceList, StatementRank.NORMAL, "P2302$77BD7FE4-C051-4776-855C-543F0CE697D0");
        List<Statement> statements = Collections.singletonList(statement);

        return statements;
    }
}
