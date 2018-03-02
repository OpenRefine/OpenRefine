package org.openrefine.wikidata.exporters;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.schema.WikibaseSchemaTest;
import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.openrefine.wikidata.updates.scheduler.UpdateSchedulerTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;

public class QuickStatementsExporterTest extends RefineTest {

     private QuickStatementsExporter exporter = new QuickStatementsExporter();
     private ItemIdValue newIdA = TestingDataGenerator.makeNewItemIdValue(1234L, "new item A");
     private ItemIdValue newIdB = TestingDataGenerator.makeNewItemIdValue(5678L, "new item B");
     private ItemIdValue qid1 = Datamodel.makeWikidataItemIdValue("Q1377");
     private ItemIdValue qid2 = Datamodel.makeWikidataItemIdValue("Q865528");
     
     private String export(ItemUpdate... itemUpdates) throws IOException {
         StringWriter writer = new StringWriter();
         exporter.translateItemList(Arrays.asList(itemUpdates), writer);
         return writer.toString();
     }
     
     @Test
     public void testSimpleProject() throws JSONException, IOException {
         Project project = this.createCSVProject(
                 "subject,inception,reference\n"+
                 "Q1377,1919,http://www.ljubljana-slovenia.com/university-ljubljana\n"+
                 "Q865528,1965,\n"+
                 "new uni,2016,http://new-uni.com/");
         project.rows.get(0).cells.set(0, TestingDataGenerator.makeMatchedCell("Q1377", "University of Ljubljana"));
         project.rows.get(1).cells.set(0, TestingDataGenerator.makeMatchedCell("Q865528", "University of Warwick"));
         project.rows.get(2).cells.set(0, TestingDataGenerator.makeNewItemCell(1234L, "new uni"));
         JSONObject serialized = WikibaseSchemaTest.jsonFromFile("data/schema/inception.json");
         WikibaseSchema schema = WikibaseSchema.reconstruct(serialized);
         project.overlayModels.put("wikibaseSchema", schema);
         Engine engine = new Engine(project);
         
         StringWriter writer = new StringWriter();
         Properties properties = new Properties();
         exporter.export(project, properties, engine, writer);
         assertEquals(
                 "Q1377\tP571\t+1919-01-01T00:00:00Z/9"+
                     "\tS854\t\"http://www.ljubljana-slovenia.com/university-ljubljana\""+
                     "\tS813\t+2018-02-28T00:00:00Z/11\n" + 
                 "Q865528\tP571\t+1965-01-01T00:00:00Z/9"+
                     "\tS813\t+2018-02-28T00:00:00Z/11\n"+
                 "CREATE\n"+
                 "LAST\tP571\t+2016-01-01T00:00:00Z/9"+
                       "\tS854\t\"http://new-uni.com/\""+
                       "\tS813\t+2018-02-28T00:00:00Z/11\n", writer.toString());
     }
     
     @Test
     public void testImpossibleScheduling() throws IOException { 
         Statement sNewAtoNewB = TestingDataGenerator.generateStatement(newIdA, newIdB);
         ItemUpdate update = new ItemUpdateBuilder(newIdA).addStatement(sNewAtoNewB).build();
         
         assertEquals(QuickStatementsExporter.impossibleSchedulingErrorMessage,
                 export(update));
     }
     
     @Test
     public void testNameDesc() throws IOException {
         ItemUpdate update = new ItemUpdateBuilder(newIdA)
                 .addLabel(Datamodel.makeMonolingualTextValue("my new item", "en"))
                 .addDescription(Datamodel.makeMonolingualTextValue("isn't it awesome?", "en"))
                 .addAlias(Datamodel.makeMonolingualTextValue("fabitem", "en"))
                 .build();
         
         assertEquals("CREATE\n"+
                 "LAST\tLen\t\"my new item\"\n"+
                 "LAST\tDen\t\"isn't it awesome?\"\n"+
                 "LAST\tAen\t\"fabitem\"\n",
                 export(update));
     }
     
     @Test
     public void testDeleteStatement() throws IOException {
         ItemUpdate update = new ItemUpdateBuilder(qid1)
                 .deleteStatement(TestingDataGenerator.generateStatement(qid1, qid2))
                 .build();
         
         assertEquals("- Q1377\tP38\tQ865528\n", export(update));
     }
     
     @Test
     public void testQualifier() throws IOException {
         Statement baseStatement = TestingDataGenerator.generateStatement(qid1, qid2);
         Statement otherStatement = TestingDataGenerator.generateStatement(qid2, qid1);
         Snak qualifierSnak = otherStatement.getClaim().getMainSnak();
         SnakGroup group = Datamodel.makeSnakGroup(Collections.singletonList(qualifierSnak));
         Claim claim = Datamodel.makeClaim(qid1, baseStatement.getClaim().getMainSnak(),
                 Collections.singletonList(group));
         Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
         ItemUpdate update = new ItemUpdateBuilder(qid1)
                 .addStatement(statement)
                 .build();
         
         assertEquals("Q1377\tP38\tQ865528\tP38\tQ1377\n", export(update));
     }
     
     @Test
     public void testNoSchema() throws IOException {
         Project project = this.createCSVProject("a,b\nc,d");
         Engine engine = new Engine(project);  
         StringWriter writer = new StringWriter();
         Properties properties = new Properties();
         exporter.export(project, properties, engine, writer);
         assertEquals(QuickStatementsExporter.noSchemaErrorMessage, writer.toString());
     }
     
     @Test
     public void testGetContentType() {
         assertEquals("text/plain", exporter.getContentType());
     }
}
