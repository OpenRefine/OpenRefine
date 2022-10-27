
package org.openrefine.wikibase.updates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A class to plan an update of an entity, after evaluating the schema but before fetching the current content of the
 * entity (this is why it does not extend {@link EntityUpdate}).
 * 
 * @author Antonin Delpeuch
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = ItemEdit.class, name = "item"),
        @Type(value = MediaInfoEdit.class, name = "mediainfo")
})
public interface EntityEdit {

    /**
     * The id of the entity being edited
     */
    @JsonProperty("subject")
    EntityIdValue getEntityId();

    /**
     * In case the subject id is not new, returns the corresponding update given the current state of the entity. Throws
     * a validation exception otherwise.
     *
     * @param entityDocument
     *            The current state of the entity. If {@link #requiresFetchingExistingState()} returns false, then this
     *            parameter may be null, as it should not be required to compute the {@link EntityUpdate}.
     */
    EntityUpdate toEntityUpdate(EntityDocument entityDocument);

    /**
     * Merges all the changes in other with this instance. Both updates should have the same subject. Changes coming
     * from `other` have priority over changes from this instance. This instance is not modified, the merged update is
     * returned instead.
     * 
     * @param otherEdit
     *            the other change that should be merged
     */
    EntityEdit merge(EntityEdit otherEdit);

    /**
     * In case the subject id is new, returns the corresponding new item document to be created. Throws a validation
     * exception otherwise.
     */
    EntityDocument toNewEntity();

    /**
     * Group a list of {@link EntityUpdate}s by subject: this is useful to make one single edit per entity.
     * 
     * @param entityDocuments
     * @return a map from entity ids to merged {@link EntityUpdate} for that id
     */
    public static Map<EntityIdValue, EntityEdit> groupBySubject(List<EntityEdit> entityDocuments) {
        Map<EntityIdValue, EntityEdit> map = new HashMap<>();
        for (EntityEdit update : entityDocuments) {
            if (update.isNull()) {
                continue;
            }

            EntityIdValue qid = update.getEntityId();
            if (map.containsKey(qid)) {
                EntityEdit oldUpdate = map.get(qid);
                map.put(qid, oldUpdate.merge(update));
            } else {
                map.put(qid, update);
            }
        }
        return map;
    }

    /**
     * Is this update about a new entity?
     */
    @JsonProperty("new")
    public default boolean isNew() {
        return EntityIdValue.SITE_LOCAL.equals(getEntityId().getSiteIri());
    }

    /**
     * @return true when this change leaves the content of the document untouched. In the case of a new entity, this
     *         could still mean making an edit to create the blank entity.
     */
    @JsonIgnore
    boolean isEmpty();

    /**
     * @return true when this change is empty and its subject is not new
     */
    @JsonIgnore
    default boolean isNull() {
        return isEmpty() && !isNew();
    }

    /**
     * @return true when performing this edit requires fetching the current contents of the entity before making the
     *         edit. By default, this is true when making non-empty edits on existing entities. But implementations may
     *         override this, to spare the request to fetch the current entity, if that information is not necessary to
     *         compute the update.
     */
    public default boolean requiresFetchingExistingState() {
        return !isEmpty() && !isNew();
    }
}
