package org.openrefine.wikidata.manifests;

import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;

public class ManifestV1Test {

    @Test
    public void testGetters() throws IOException, ManifestException {
        String json = TestingData.jsonFromFile("manifest/wikidata-manifest-v1.0.json");
        Manifest manifest = ManifestParser.parse(json);
        assertEquals("1.0", manifest.getVersion());
        assertEquals("Wikidata", manifest.getName());
        assertEquals("https://www.wikidata.org/w/api.php", manifest.getMediaWikiApiEndpoint());
        assertEquals("http://www.wikidata.org/entity/", manifest.getSiteIri());
        assertEquals(5, manifest.getMaxlag());
        assertEquals("P31", manifest.getInstanceOfPid());
        assertEquals("P279", manifest.getSubclassOfPid());
        assertEquals("https://wikidata.reconci.link/${lang}/api", manifest.getReconServiceEndpoint());
        assertEquals("P2302", manifest.getConstraintsRelatedId("property_constraint_pid"));
        assertEquals("Q19474404", manifest.getConstraintsRelatedId("single_value_constraint_qid"));
        assertEquals("([[:toollabs:editgroups/b/OR/${batch_id}|details]])", manifest.getEditGroupsUrlSchema());
    }

    @Test
    public void testInvalidManifestFormat() {
        String invalidJson = "{invalid";
        try {
            ManifestParser.parse(invalidJson);
        } catch (ManifestException e) {
            assertEquals("invalid manifest format", e.getMessage());
        }
    }

    @Test
    public void testMissingVersion() {
        String missingVersion = "{\"a\": \"b\"}";
        try {
            ManifestParser.parse(missingVersion);
        } catch (ManifestException e) {
            assertEquals("invalid manifest format, version is missing", e.getMessage());
        }
    }

    @Test
    public void testInvalidVersion() {
        String invalidVersion1 = "{\"version\": \"a1.0\"}";
        try {
            ManifestParser.parse(invalidVersion1);
        } catch (ManifestException e) {
            assertEquals("invalid version: a1.0", e.getMessage());
        }

        String invalidVersion2 = "{\"version\": \"1.1a\"}";
        try {
            ManifestParser.parse(invalidVersion2);
        } catch (ManifestException e) {
            assertEquals("invalid version: 1.1a", e.getMessage());
        }
    }

    @Test
    public void testUnsupportedVersion() {
        String unsupportedVersion = "{\"version\": \"2.0\"}";
        try {
            ManifestParser.parse(unsupportedVersion);
        } catch (ManifestException e) {
            assertEquals("unsupported manifest version: 2.0", e.getMessage());
        }
    }

}
