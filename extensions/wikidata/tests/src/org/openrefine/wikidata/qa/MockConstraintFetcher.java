package org.openrefine.wikidata.qa;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public class MockConstraintFetcher implements ConstraintFetcher {
    
    public static PropertyIdValue pidWithInverse = Datamodel.makeWikidataPropertyIdValue("P350");
    public static PropertyIdValue inversePid = Datamodel.makeWikidataPropertyIdValue("P57");
    public static PropertyIdValue allowedQualifierPid = Datamodel.makeWikidataPropertyIdValue("P34");
    public static PropertyIdValue mandatoryQualifierPid = Datamodel.makeWikidataPropertyIdValue("P97");
    
    public static PropertyIdValue mainSnakPid = Datamodel.makeWikidataPropertyIdValue("P1234");
    public static PropertyIdValue qualifierPid = Datamodel.makeWikidataPropertyIdValue("P987");
    public static PropertyIdValue referencePid = Datamodel.makeWikidataPropertyIdValue("P384");

    @Override
    public String getFormatRegex(PropertyIdValue pid) {
        return "[1-9]\\d+";
    }

    /**
     * This constraint is purposely left inconsistent (the inverse
     * constraint holds only on one side).
     */
    @Override
    public PropertyIdValue getInversePid(PropertyIdValue pid) {
        if (pidWithInverse.equals(pid)) {
            return inversePid;
        }
        return null;
    }

    @Override
    public boolean isForValuesOnly(PropertyIdValue pid) {
        return mainSnakPid.equals(pid);
    }

    @Override
    public boolean isForQualifiersOnly(PropertyIdValue pid) {
        return qualifierPid.equals(pid);
    }

    @Override
    public boolean isForReferencesOnly(PropertyIdValue pid) {
        return referencePid.equals(pid);
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
    public boolean hasDistinctValues(PropertyIdValue pid) {
        return true;
    }

}
