
package org.openrefine.wikibase.manifests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class ManifestV1 implements Manifest {

    private String version;
    private String name;
    private String siteIri;
    private int maxlag;
    private String instanceOfPid;
    private String subclassOfPid;
    private String mediaWikiApiEndpoint;
    private String reconServiceEndpoint;
    private String editGroupsUrlSchema;
    private String tagTemplate;
    private int maxEditsPerMinute;

    private Map<String, String> constraintsRelatedIdMap = new HashMap<>();

    public ManifestV1(JsonNode manifest) {
        version = manifest.path("version").textValue();

        JsonNode mediawiki = manifest.path("mediawiki");
        name = mediawiki.path("name").textValue();
        mediaWikiApiEndpoint = mediawiki.path("api").textValue();

        JsonNode wikibase = manifest.path("wikibase");
        siteIri = wikibase.path("site_iri").textValue();
        maxlag = wikibase.path("maxlag").intValue();
        tagTemplate = wikibase.path("tag").isTextual() ? wikibase.path("tag").asText() : Manifest.DEFAULT_TAG_TEMPLATE;
        maxEditsPerMinute = wikibase.path("max_edits_per_minute").isNumber() ? wikibase.path("max_edits_per_minute").intValue()
                : Manifest.DEFAULT_MAX_EDITS_PER_MINUTE;
        JsonNode properties = wikibase.path("properties");
        instanceOfPid = properties.path("instance_of").textValue();
        subclassOfPid = properties.path("subclass_of").textValue();

        JsonNode constraints = wikibase.path("constraints");
        Iterator<Map.Entry<String, JsonNode>> fields = constraints.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            String value = entry.getValue().textValue();
            constraintsRelatedIdMap.put(name, value);
        }

        JsonNode reconciliation = manifest.path("reconciliation");
        reconServiceEndpoint = reconciliation.path("endpoint").textValue();

        JsonNode editGroups = manifest.path("editgroups");
        editGroupsUrlSchema = editGroups.path("url_schema").textValue();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSiteIri() {
        return siteIri;
    }

    @Override
    public int getMaxlag() {
        return maxlag;
    }

    @Override
    public String getInstanceOfPid() {
        return instanceOfPid;
    }

    @Override
    public String getSubclassOfPid() {
        return subclassOfPid;
    }

    @Override
    public String getMediaWikiApiEndpoint() {
        return mediaWikiApiEndpoint;
    }

    @Override
    public String getReconServiceEndpoint() {
        return reconServiceEndpoint;
    }

    @Override
    public String getConstraintsRelatedId(String name) {
        return constraintsRelatedIdMap.get(name);
    }

    @Override
    public String getEditGroupsUrlSchema() {
        return editGroupsUrlSchema;
    }

    @Override
    public String getReconServiceEndpoint(String entityType) {
        if (ITEM_TYPE.equals(entityType)) {
            return reconServiceEndpoint;
        }
        return null;
    }

    @Override
    public String getEntityTypeSiteIri(String entityType) {
        if (ITEM_TYPE.equals(entityType) || PROPERTY_TYPE.equals(entityType)) {
            return siteIri;
        }
        return null;
    }

    @Override
    public String getMediaWikiApiEndpoint(String entityType) {
        return getMediaWikiApiEndpoint();
    }

    @Override
    public List<String> getAvailableEntityTypes() {
        return Arrays.asList(ITEM_TYPE, PROPERTY_TYPE);
    }

    @Override
    public boolean hideStructuredFieldsInMediaInfo() {
        return false;
    }

    @Override
    public String getTagTemplate() {
        return tagTemplate;
    }

    @Override
    public int getMaxEditsPerMinute() {
        return maxEditsPerMinute;
    }

}
