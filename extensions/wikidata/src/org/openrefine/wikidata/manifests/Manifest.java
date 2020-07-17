package org.openrefine.wikidata.manifests;

public interface Manifest {

    String getVersion();

    String getName();

    String getEntityPrefix();

    String getMediaWikiApiEndpoint();

    String getReconServiceEndpoint();

    String getPropertyConstraintPid();

    Constraints getConstraints();

}
