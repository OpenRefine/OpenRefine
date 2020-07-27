package org.openrefine.wikidata.manifests;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ManifestV1 implements Manifest {

    private String version;
    private String name;
    private String entityPrefix;
    private String mediaWikiApiEndpoint;
    private String reconServiceEndpoint;

    private Map<String, String> constraintsRelatedIdMap = new HashMap<>();

    public ManifestV1(JsonNode manifest) {
        version = manifest.path("version").textValue();

        JsonNode mediawiki = manifest.path("mediawiki");
        name = mediawiki.path("name").textValue();
        mediaWikiApiEndpoint = mediawiki.path("api").textValue();

        JsonNode wikibase = manifest.path("wikibase");
        JsonNode properties = wikibase.path("properties");
        entityPrefix = properties.path("entity_prefix").textValue();
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
    public String getEntityPrefix() {
        return entityPrefix;
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

}
