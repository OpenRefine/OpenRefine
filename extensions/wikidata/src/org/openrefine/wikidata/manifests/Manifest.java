package org.openrefine.wikidata.manifests;

public interface Manifest {

    String getVersion();

    String getName();

    String getSiteIri();

    String getInstanceOfPid();

    String getSubclassOfPid();

    String getMediaWikiApiEndpoint();

    String getReconServiceEndpoint();

    String getConstraintsRelatedId(String name);

}
