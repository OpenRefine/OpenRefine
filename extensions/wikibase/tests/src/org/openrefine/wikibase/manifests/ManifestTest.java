
package org.openrefine.wikibase.manifests;

import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.Arrays;

public class ManifestTest {

    @Test
    public void testV1() throws IOException, ManifestException {
        String json = TestingData.jsonFromFile("manifest/wikidata-manifest-v1.0.json");
        Manifest manifest = ManifestParser.parse(json);
        assertEquals("1.0", manifest.getVersion());
        assertEquals("Wikidata", manifest.getName());
        assertEquals("https://www.wikidata.org/w/api.php", manifest.getMediaWikiApiEndpoint());
        assertEquals("https://www.wikidata.org/w/api.php", manifest.getMediaWikiApiEndpoint("item"));
        assertEquals("http://www.wikidata.org/entity/", manifest.getSiteIri());
        assertEquals(5, manifest.getMaxlag());
        assertEquals("P31", manifest.getInstanceOfPid());
        assertEquals("P279", manifest.getSubclassOfPid());
        assertEquals("https://wikidata.reconci.link/${lang}/api", manifest.getReconServiceEndpoint());
        assertEquals("https://wikidata.reconci.link/${lang}/api", manifest.getReconServiceEndpoint(Manifest.ITEM_TYPE));
        assertNull(manifest.getReconServiceEndpoint(Manifest.MEDIAINFO_TYPE));
        assertEquals(Arrays.asList(Manifest.ITEM_TYPE, Manifest.PROPERTY_TYPE), manifest.getAvailableEntityTypes());
        assertEquals("http://www.wikidata.org/entity/", manifest.getEntityTypeSiteIri(Manifest.ITEM_TYPE));
        assertNull(manifest.getEntityTypeSiteIri(Manifest.MEDIAINFO_TYPE));
        assertEquals("P2302", manifest.getConstraintsRelatedId("property_constraint_pid"));
        assertEquals("Q19474404", manifest.getConstraintsRelatedId("single_value_constraint_qid"));
        assertEquals("([[:toollabs:editgroups/b/OR/${batch_id}|details]])", manifest.getEditGroupsUrlSchema());
    }

    @Test
    public void testV2() throws IOException, ManifestException {
        String json = TestingData.jsonFromFile("manifest/commons-manifest-v2.0.json");
        Manifest manifest = ManifestParser.parse(json);
        assertEquals("2.0", manifest.getVersion());
        assertEquals("Wikimedia Commons", manifest.getName());
        assertEquals("https://commons.wikimedia.org/w/api.php", manifest.getMediaWikiApiEndpoint());
        assertEquals("https://www.wikidata.org/w/api.php", manifest.getMediaWikiApiEndpoint("item"));
        assertEquals("https://commons.wikimedia.org/entity/", manifest.getSiteIri());
        assertEquals(5, manifest.getMaxlag());
        assertEquals("P31", manifest.getInstanceOfPid());
        assertEquals("P279", manifest.getSubclassOfPid());
        assertEquals("https://commonsreconcile.toolforge.org/${lang}/api", manifest.getReconServiceEndpoint(Manifest.MEDIAINFO_TYPE));
        assertEquals("https://wikidata.reconci.link/${lang}/api", manifest.getReconServiceEndpoint(Manifest.ITEM_TYPE));
        assertNull(manifest.getReconServiceEndpoint(Manifest.PROPERTY_TYPE));
        assertEquals(Arrays.asList(Manifest.ITEM_TYPE, Manifest.PROPERTY_TYPE, Manifest.MEDIAINFO_TYPE),
                manifest.getAvailableEntityTypes());
        assertEquals("http://www.wikidata.org/entity/", manifest.getEntityTypeSiteIri(Manifest.ITEM_TYPE));
        assertEquals("https://commons.wikimedia.org/entity/", manifest.getEntityTypeSiteIri(Manifest.MEDIAINFO_TYPE));
        assertEquals("P2302", manifest.getConstraintsRelatedId("property_constraint_pid"));
        assertEquals("([[:toollabs:editgroups-commons/b/OR/${batch_id}|details]])", manifest.getEditGroupsUrlSchema());
        assertFalse(manifest.hideStructuredFieldsInMediaInfo());
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
        String unsupportedVersion = "{\"version\": \"3.0\"}";
        try {
            ManifestParser.parse(unsupportedVersion);
        } catch (ManifestException e) {
            assertEquals("unsupported manifest version: 3.0", e.getMessage());
        }
    }

}
