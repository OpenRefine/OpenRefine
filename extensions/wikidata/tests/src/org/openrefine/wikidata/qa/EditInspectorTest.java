
package org.openrefine.wikidata.qa;

import org.openrefine.wikidata.manifests.Manifest;
import org.openrefine.wikidata.manifests.ManifestParser;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class EditInspectorTest {

    private static final int scrutinizerCount = 22;
    private static final int scrutinizerNotDependingOnPropertyConstraintCount = 7;

    @Test
    public void testNoScrutinizerSkipped() throws Exception {
        String manifestJson = TestingData.jsonFromFile("manifest/wikidata-manifest-v1.0.json");
        Manifest manifest = ManifestParser.parse(manifestJson);
        EditInspector editInspector = new EditInspector(new QAWarningStore(), manifest);
        assertEquals(editInspector.scrutinizers.size(), scrutinizerCount);
    }

    @Test
    public void toSkipScrutinizerDependingOnConstraintPropertyPid1() throws Exception {
        String manifestJson = TestingData.jsonFromFile("manifest/wikidata-manifest-v1.0-without-constraints.json");
        Manifest manifest = ManifestParser.parse(manifestJson);
        EditInspector editInspector = new EditInspector(new QAWarningStore(), manifest);
        assertEquals(editInspector.scrutinizers.size(), scrutinizerNotDependingOnPropertyConstraintCount);
    }

    @Test
    public void toSkipScrutinizerDependingOnConstraintPropertyPid2() throws Exception {
        String manifestJson = TestingData.jsonFromFile("manifest/wikidata-manifest-v1.0-missing-property-constraint-pid.json");
        Manifest manifest = ManifestParser.parse(manifestJson);
        EditInspector editInspector = new EditInspector(new QAWarningStore(), manifest);
        assertEquals(editInspector.scrutinizers.size(), scrutinizerNotDependingOnPropertyConstraintCount);
    }
}
