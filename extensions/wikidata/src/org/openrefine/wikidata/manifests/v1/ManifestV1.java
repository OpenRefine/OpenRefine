package org.openrefine.wikidata.manifests.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.openrefine.wikidata.manifests.Constraints;
import org.openrefine.wikidata.manifests.Manifest;

public class ManifestV1 implements Manifest {

    private String version;
    private String name;
    private String entityPrefix;
    private String mediaWikiApiEndpoint;
    private String reconServiceEndpoint;
    private String propertyConstraintPid;
    private Constraints constraints;

    public ManifestV1(JsonNode manifest) {
        version = manifest.path("version").textValue();

        JsonNode mediawiki = manifest.path("mediawiki");
        name = mediawiki.path("name").textValue();
        mediaWikiApiEndpoint = mediawiki.path("api").textValue();

        JsonNode wikibase = manifest.path("wikibase");
        constraints = new ConstraintsV1((ArrayNode) wikibase.path("constraints"));
        JsonNode properties = wikibase.path("properties");
        propertyConstraintPid = properties.path("property_constraint").textValue();
        entityPrefix = properties.path("entity_prefix").textValue();

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
    public String getPropertyConstraintPid() {
        return propertyConstraintPid;
    }

    @Override
    public Constraints getConstraints() {
        return constraints;
    }
}
