package org.openrefine.wikidata.manifests;

import org.openrefine.wikidata.manifests.constraints.AllowedEntityTypesConstraint;
import org.openrefine.wikidata.manifests.constraints.TypeConstraint;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;

public class ManifestTest {

    @Test
    public void test() throws IOException, ManifestException {
        String json = TestingData.jsonFromFile("manifest/manifest-v1.0.json");
        ManifestManager manager = ManifestManager.getInstance();
        manager.updateManifest(json);
        Manifest manifest = manager.getManifest();
        assertEquals("1.0", manifest.getVersion());
        assertEquals("Wikidata", manifest.getName());
        assertEquals("https://www.wikidata.org/w/api.php", manifest.getMediaWikiApiEndpoint());
        assertEquals("http://www.wikidata.org/entity/", manifest.getIri());
        assertEquals("https://wdreconcile.toolforge.org/en/api", manifest.getReconServiceEndpoint());
        assertEquals("P2302", manifest.getPropertyConstraintPid());

        Constraints constraints = manifest.getConstraints();

        // TODO: more tests
        AllowedEntityTypesConstraint allowedEntityTypesConstraint = constraints.getAllowedEntityTypesConstraint();
        assertEquals("Q52004125", allowedEntityTypesConstraint.getQid());

        TypeConstraint typeConstraint = constraints.getTypeConstraint();
        assertEquals("P2308", typeConstraint.getKlass());
    }
}
