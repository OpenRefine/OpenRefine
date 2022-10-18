
package org.openrefine.wikibase.updates;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class StatementEditTest {

    Statement statement = TestingData.generateStatement(TestingData.existingId, TestingData.newIdA);
    StatementMerger strategy = new PropertyOnlyStatementMerger();
    StatementEditingMode mode = StatementEditingMode.ADD_OR_MERGE;
    StatementEdit SUT = new StatementEdit(statement, strategy, mode);

    @Test
    public void testGetters() {
        assertEquals(SUT.getStatement(), statement);
        assertEquals(SUT.getMerger(), strategy);
        assertEquals(SUT.getMode(), mode);
        assertEquals(SUT.getPropertyId(), statement.getMainSnak().getPropertyId());
    }

    @Test
    public void testWithSubjectId() {
        ItemIdValue otherId = Datamodel.makeWikidataItemIdValue("Q898");
        StatementEdit translated = SUT.withSubjectId(otherId);

        assertEquals(translated.getMerger(), SUT.getMerger());
        assertEquals(translated.getMode(), SUT.getMode());
        assertEquals(translated.getStatement().getClaim().getSubject(), otherId);
    }

    @Test
    public void testToString() {
        assertEquals(SUT.toString(),
                "Add statement [[ID ] http://www.wikidata.org/entity/Q43 (item): http://www.wikidata.org/entity/P38 :: new item (reconciled from 1234)\n"
                        + ", PropertyOnlyStatementMerger]");
    }

    @Test
    public void testEquality() {
        StatementEdit other = new StatementEdit(statement, strategy, mode);
        assertEquals(SUT, other);
        assertNotEquals(SUT, statement);
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        TestUtils.assertEqualsAsJson(ParsingUtilities.mapper.writeValueAsString(SUT), "{\n"
                + "       \"mergingStrategy\" : {\n"
                + "         \"type\" : \"property\"\n"
                + "       },\n"
                + "       \"mode\" : \"add_or_merge\",\n"
                + "       \"statement\" : {\n"
                + "         \"mainsnak\" : {\n"
                + "           \"datatype\" : \"wikibase-item\",\n"
                + "           \"datavalue\" : {\n"
                + "             \"entityType\" : \"http://www.wikidata.org/ontology#Item\",\n"
                + "             \"id\" : \"Q1234\",\n"
                + "             \"iri\" : \"http://localhost/entity/Q1234\",\n"
                + "             \"label\" : \"new item A\",\n"
                + "             \"placeholder\" : true,\n"
                + "             \"reconInternalId\" : 1234,\n"
                + "             \"siteIri\" : \"http://localhost/entity/\",\n"
                + "             \"types\" : [ ]\n"
                + "           },\n"
                + "           \"property\" : \"P38\",\n"
                + "           \"snaktype\" : \"value\"\n"
                + "         },\n"
                + "         \"rank\" : \"normal\",\n"
                + "         \"type\" : \"statement\"\n"
                + "       }\n"
                + "     }");
    }
}
