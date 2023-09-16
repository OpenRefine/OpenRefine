
package org.openrefine.wikibase.manifests;

import java.util.List;

/**
 * A configuration object for a Wikibase instance. The deserialization of this object is versioned, via
 * {@link org.openrefine.wikibase.manifests.ManifestParser}.
 * 
 * @author Lu Liu, Antonin Delpeuch
 *
 */
public interface Manifest {

    public static final String ITEM_TYPE = "item";
    public static final String PROPERTY_TYPE = "property";
    public static final String MEDIAINFO_TYPE = "mediainfo";
    public static final int DEFAULT_MAX_EDITS_PER_MINUTE = 60;
    public static final String DEFAULT_TAG_TEMPLATE = "openrefine-${version}";

    /**
     * The version of the manifest object, which determines its JSON format.
     */
    String getVersion();

    /**
     * The name of the Wikibase instance, displayed in the UI.
     */
    String getName();

    /**
     * The RDF serialization prefix for entities stored in this instance.
     */
    String getSiteIri();

    /**
     * The recommended `maxlag` value for edits on this instance.
     */
    int getMaxlag();

    /**
     * The tag to apply to edits made from OpenRefine. If the string contains the ${version} string, it should be
     * replaced by the major.minor OpenRefine version.
     */
    String getTagTemplate();

    /**
     * The maximum number of edits to do per minute on this Wikibase instance. Set to zero to disable any throttling.
     */
    int getMaxEditsPerMinute();

    /**
     * The property id used to link an entity to the entity representing its type (class). In Wikidata, this is P31
     * (instance of)
     */
    String getInstanceOfPid();

    /**
     * The property id used to link a class to its superclasses. In Wikidata, this is P279 (subclass of)
     */
    String getSubclassOfPid();

    /**
     * The MediaWiki API endpoint of this Wikibase instance.
     */
    String getMediaWikiApiEndpoint();

    /**
     * The reconciliation service for entities in this Wikibase instance.
     * 
     * @deprecated use {@link #getReconServiceEndpoint(String)} with "item" as argument
     */
    String getReconServiceEndpoint();

    /**
     * Get the reconciliation service endpoint for a given entity type supported by this Wikibase instance.
     * 
     * @param entityType
     * @return null if there is no recon service for this entity type.
     */
    String getReconServiceEndpoint(String entityType);

    /**
     * Get the MediaWiki endpoint of the source Wikibase for the entity type. This can be different than the MediaWiki
     * endpoint for this Wikibase instance, when federation is used.
     * 
     * @param entityType
     * @return null if there is no recon service for this entity type.
     */
    String getMediaWikiApiEndpoint(String entityType);

    /**
     * Gets the site IRI used for a particular entity type. - if the entity type is editable on this Wikibase instance,
     * then it should be identical to the site IRI for this instance. - if the entity type is federated from another
     * instance, then it should be the site IRI for that instance.
     * 
     * @param entityType
     * @return null if the entity type is not supported by the Wikibase instance
     */
    String getEntityTypeSiteIri(String entityType);

    /**
     * The list of all entity types in use on this instance.
     */
    List<String> getAvailableEntityTypes();

    /**
     * Only useful for Wikibase instances to which one can upload files: this is set to true when the Wikibase instance
     * does not support structured data in the form of MediaInfo entities. In this case, OpenRefine will still offer
     * editing those files, but hide the Captions and Statements fields.
     */
    boolean hideStructuredFieldsInMediaInfo();

    /**
     * Returns an entity or property id used in the WikibaseQualityConstraints extension.
     * 
     * @param name
     *            our internal identifier for the entity id
     * @return the entity id
     */
    String getConstraintsRelatedId(String name);

    /**
     * Returns the template that should be inserted in edit summaries for edits to be tracked by EditGroups.
     */
    String getEditGroupsUrlSchema();
}
