
package org.openrefine.wikibase.schema.strategies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.google.refine.util.TestUtils;

public class QualifiersStatementMergerTests {

    String qualifierPid = "P1234";
    QualifiersStatementMerger SUTnoPids = new QualifiersStatementMerger(new StrictValueMatcher(), null);
    QualifiersStatementMerger SUTwithPids = new QualifiersStatementMerger(new StrictValueMatcher(), Collections.singleton(qualifierPid));

    ItemIdValue qidA = Datamodel.makeWikidataItemIdValue("Q3489");
    ItemIdValue qidB = Datamodel.makeWikidataItemIdValue("Q4903");
    PropertyIdValue discrimatingQualifierPid = Datamodel.makeWikidataPropertyIdValue(qualifierPid);
    PropertyIdValue otherPid = Datamodel.makeWikidataPropertyIdValue("P898");
    PropertyIdValue otherPid2 = Datamodel.makeWikidataPropertyIdValue("P33333");
    Snak mainSnak = Datamodel.makeValueSnak(otherPid, qidA);
    SnakGroup discriminatingQualifier1 = Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(discrimatingQualifierPid, qidA)));
    SnakGroup discriminatingQualifier2 = Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(discrimatingQualifierPid, qidB)));
    SnakGroup nonDiscriminatingQualifier1 = Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(otherPid, qidA)));
    SnakGroup nonDiscriminatingQualifier2 = Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(otherPid, qidB)));
    SnakGroup nonDiscriminatingQualifier3 = Datamodel.makeSnakGroup(
            Collections.singletonList(Datamodel.makeValueSnak(otherPid2, qidB)));
    SnakGroup nonDiscriminatingQualifier1and2 = Datamodel.makeSnakGroup(
            Arrays.asList(Datamodel.makeValueSnak(otherPid, qidA), Datamodel.makeValueSnak(otherPid, qidB)));

    Statement statementA = statement(Datamodel.makeClaim(qidA, mainSnak, Collections.singletonList(discriminatingQualifier1)));
    Statement statementB = statement(Datamodel.makeClaim(qidA, mainSnak, Collections.singletonList(discriminatingQualifier2)));
    Statement statementC = statement(
            Datamodel.makeClaim(qidA, mainSnak, Arrays.asList(nonDiscriminatingQualifier1, discriminatingQualifier1)));
    Statement statementD = statement(Datamodel.makeClaim(qidA, mainSnak, Arrays.asList(
            nonDiscriminatingQualifier2, discriminatingQualifier1, nonDiscriminatingQualifier3)));
    Statement statementE = statement(Datamodel.makeClaim(qidA, mainSnak, Collections.emptyList()));

    @Test
    public void testMatchNoPids() {
        assertFalse(SUTnoPids.match(statementA, statementB));
        assertFalse(SUTnoPids.match(statementA, statementC));
        assertFalse(SUTnoPids.match(statementA, statementE));
        assertTrue(SUTnoPids.match(statementA, statementA));
    }

    @Test
    public void testMergeNoPids() {
        assertEquals(SUTnoPids.merge(statementA, statementA), statementA);
    }

    @Test
    public void testMatchWithPids() {
        assertFalse(SUTwithPids.match(statementA, statementB));
        assertTrue(SUTwithPids.match(statementA, statementC));
        assertTrue(SUTwithPids.match(statementA, statementD));
        assertFalse(SUTwithPids.match(statementA, statementE));
        assertTrue(SUTwithPids.match(statementC, statementD));
    }

    @Test
    public void testMergeWithPids() {
        assertEquals(SUTwithPids.merge(statementA, statementA), statementA);
        assertEquals(SUTwithPids.merge(statementC, statementC), statementC);
        assertEquals(SUTwithPids.merge(statementC, statementD), statement(
                Datamodel.makeClaim(qidA, mainSnak, Arrays.asList(
                        nonDiscriminatingQualifier1and2,
                        discriminatingQualifier1,
                        nonDiscriminatingQualifier3))));
    }

    @Test
    public void testGetters() {
        assertEquals(SUTnoPids.getValueMatcher(), new StrictValueMatcher());
        assertEquals(SUTnoPids.getPids(), Collections.emptySet());
    }

    @Test
    public void testToString() {
        assertEquals(SUTwithPids.toString(), "QualifiersStatementMerger [valueMatcher=StrictValueMatcher, pids=[P1234]]");
    }

    @Test
    public void testEquality() {
        assertNotEquals(SUTnoPids, SUTwithPids);
        assertEquals(SUTnoPids, new QualifiersStatementMerger(new StrictValueMatcher(), Collections.emptySet()));
    }

    @Test
    public void testHashCode() {
        assertNotEquals(SUTnoPids.hashCode(), SUTwithPids.hashCode());
        assertEquals(SUTnoPids.hashCode(), new QualifiersStatementMerger(new StrictValueMatcher(), Collections.emptySet()).hashCode());
    }

    @Test
    public void testJsonSerialization() {
        TestUtils.isSerializedTo(SUTnoPids, "{\n"
                + "       \"pids\" : [ ],\n"
                + "       \"type\" : \"qualifiers\",\n"
                + "       \"valueMatcher\" : {\n"
                + "         \"type\" : \"strict\"\n"
                + "       }\n"
                + "     }");
        TestUtils.isSerializedTo(SUTwithPids, "{\n"
                + "       \"pids\" : [ \"P1234\" ],\n"
                + "       \"type\" : \"qualifiers\",\n"
                + "       \"valueMatcher\" : {\n"
                + "         \"type\" : \"strict\"\n"
                + "       }\n"
                + "     }");
    }

    public Statement statement(Claim claim) {
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

}
