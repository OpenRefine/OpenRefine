package org.openrefine.wikidata.manifests;

public interface Manifest {

    String getVersion();

    String getName();

    String getSiteIri();

    int getMaxlag();

    String getInstanceOfPid();

    String getSubclassOfPid();

    String getMediaWikiApiEndpoint();

    String getReconServiceEndpoint();

    String getConstraintsRelatedId(String name);

}
