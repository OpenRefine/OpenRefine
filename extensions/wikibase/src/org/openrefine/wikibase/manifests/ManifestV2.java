
package org.openrefine.wikibase.manifests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;

public class ManifestV2 implements Manifest {

    private String version;
    private String name;
    private String siteIri;
    private int maxlag;
    private int maxEditsPerMinute;
    private String instanceOfPid;
    private String subclassOfPid;
    private String mediaWikiApiEndpoint;
    private String editGroupsUrlSchema;
    private String tagTemplate;
    private boolean hideStructuredFieldsInMediaInfo;

    private Map<String, EntityTypeSettings> entityTypeSettings;

    private Map<String, String> constraintsRelatedIdMap = new HashMap<>();

    public ManifestV2(JsonNode manifest) throws JsonParseException, JsonMappingException, IOException {
        version = manifest.path("version").textValue();

        JsonNode mediawiki = manifest.path("mediawiki");
        name = mediawiki.path("name").textValue();
        mediaWikiApiEndpoint = mediawiki.path("api").textValue();

        JsonNode wikibase = manifest.path("wikibase");
        siteIri = wikibase.path("site_iri").textValue();
        maxlag = wikibase.path("maxlag").intValue();
        tagTemplate = wikibase.path("tag").isTextual() ? wikibase.path("tag").asText() : Manifest.DEFAULT_TAG_TEMPLATE;
        maxEditsPerMinute = wikibase.path("max_edits_per_minute").isNumber() ? wikibase.path("max_edits_per_minute").asInt()
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

        JsonNode entityTypesJson = manifest.path("entity_types");
        entityTypeSettings = com.google.refine.util.ParsingUtilities.mapper.readValue(
                ParsingUtilities.mapper.treeAsTokens(entityTypesJson),
                new TypeReference<Map<String, EntityTypeSettings>>() {
                });
        JsonNode editGroups = manifest.path("editgroups");
        editGroupsUrlSchema = editGroups.path("url_schema").textValue();
        hideStructuredFieldsInMediaInfo = editGroups.path("hide_structured_fields_in_mediainfo").asBoolean(false);
    }

    private static class EntityTypeSettings {

        protected String siteIri;
        protected String reconEndpoint;
        protected String mediaWikiApi;

        @JsonCreator
        protected EntityTypeSettings(
                @JsonProperty("site_iri") String siteIri,
                @JsonProperty("reconciliation_endpoint") String reconEndpoint,
                @JsonProperty("mediawiki_api") String mediawikiEndpoint) {
            this.siteIri = siteIri;
            this.reconEndpoint = reconEndpoint;
            this.mediaWikiApi = mediawikiEndpoint;
        }
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
        return getReconServiceEndpoint(ITEM_TYPE);
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
        EntityTypeSettings setting = entityTypeSettings.get(entityType);
        if (setting == null) {
            return null;
        }
        return setting.reconEndpoint;
    }

    @Override
    public String getEntityTypeSiteIri(String entityType) {
        EntityTypeSettings setting = entityTypeSettings.get(entityType);
        if (setting == null) {
            return null;
        }
        return setting.siteIri;
    }

    @Override
    public String getMediaWikiApiEndpoint(String entityType) {
        EntityTypeSettings setting = entityTypeSettings.get(entityType);
        if (setting == null) {
            return null;
        }
        return setting.mediaWikiApi != null ? setting.mediaWikiApi : getMediaWikiApiEndpoint();
    }

    @Override
    public List<String> getAvailableEntityTypes() {
        return entityTypeSettings.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public boolean hideStructuredFieldsInMediaInfo() {
        return hideStructuredFieldsInMediaInfo;
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
