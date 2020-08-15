package org.openrefine.wikidata.manifests;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ManifestV1 implements Manifest {

    private String version;
    private String name;
    private String siteIri;
    private int maxlag;
    private String instanceOfPid;
    private String subclassOfPid;
    private String mediaWikiApiEndpoint;
    private String reconServiceEndpoint;

    private Map<String, String> constraintsRelatedIdMap = new HashMap<>();

    public ManifestV1(JsonNode manifest) {
        version = manifest.path("version").textValue();

        JsonNode mediawiki = manifest.path("mediawiki");
        name = mediawiki.path("name").textValue();
        mediaWikiApiEndpoint = mediawiki.path("api").textValue();

        JsonNode wikibase = manifest.path("wikibase");
        siteIri = wikibase.path("site_iri").textValue();
        maxlag = wikibase.path("maxlag").intValue();
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

}
