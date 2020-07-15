package org.openrefine.wikidata.manifests;

public interface Manifest {

    String getVersion();

    String getName();

    String getIri();

    String getMediaWikiApiEndpoint();

    String getReconServiceEndpoint();

    String getPropertyConstraintPid();

    Constraints getConstraints();

}
