package org.openrefine.wikidata.manifests.v1_0;

import com.fasterxml.jackson.databind.JsonNode;
import org.openrefine.wikidata.manifests.Constraints;
import org.openrefine.wikidata.manifests.Manifest;

public class ManifestV1_0 implements Manifest {

    private String name;
    private String iri;
    private String mediaWikiApiEndpoint;
    private String reconServiceEndpoint;
    private String propertyConstraintPid;
    private Constraints constraints;

    public ManifestV1_0(JsonNode manifest) {
        JsonNode mediawiki = manifest.path("mediawiki");
        name = mediawiki.path("name").textValue();
        mediaWikiApiEndpoint = mediawiki.path("api").textValue();

        JsonNode wikibase = manifest.path("wikibase");
        iri = wikibase.path("iri").textValue();
        constraints = new ConstraintsV1_0(wikibase.path("constraints"));
        JsonNode properties = wikibase.path("properties");
        propertyConstraintPid = properties.path("property_constraint").textValue();

        JsonNode reconciliation = manifest.path("reconciliation");
        reconServiceEndpoint = reconciliation.path("endpoint").textValue();
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getIri() {
        return iri;
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
