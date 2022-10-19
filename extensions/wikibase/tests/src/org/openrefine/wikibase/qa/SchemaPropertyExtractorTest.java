
package org.openrefine.wikibase.qa;

import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SchemaPropertyExtractorTest {

    public WikibaseSchema schema;
    String serialized;

    @BeforeTest
    public void initialize() throws IOException {
        serialized = TestingData.jsonFromFile("schema/inception.json");
    }

    public Set<PropertyIdValue> makePropertySet(String... pids) {
        Set<PropertyIdValue> propertyIdValues = new HashSet<>();
        for (String pid : pids) {
            PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue(pid);
            propertyIdValues.add(propertyIdValue);
        }
        return propertyIdValues;
    }

    @Test
    public void testGetAllProperties() throws IOException {
        schema = WikibaseSchema.reconstruct(serialized);
        SchemaPropertyExtractor extractor = new SchemaPropertyExtractor();
        Set<PropertyIdValue> propertyIdValues = extractor.getAllProperties(schema);
        Assert.assertEquals(propertyIdValues, makePropertySet("P813", "P571", "P854"));
    }

    @Test
    public void testNoProperties() {
        schema = new WikibaseSchema();
        SchemaPropertyExtractor extractor = new SchemaPropertyExtractor();
        Set<PropertyIdValue> propertyIdValues = extractor.getAllProperties(schema);
        Assert.assertEquals(propertyIdValues, new HashSet<>());
    }

}
