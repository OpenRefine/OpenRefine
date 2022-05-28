package org.openrefine.wikidata.updates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoDocument;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;

/**
 * Represents a candidate edit on a MediaInfo entity.
 * 
 * @author Antonin Delpeuch
 *
 */
public class MediaInfoEdit extends LabeledStatementEntityEdit {

    /**
     * Constructor.
     * 
     * @param id
     *            the subject of the document. It can be a reconciled entity value for
     *            new entities.
     * @param statements
     *            the statements to change on the entity.
     * @param labels
     *            the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     */
	public MediaInfoEdit(EntityIdValue id, List<StatementEdit> statements, Set<MonolingualTextValue> labels,
			Set<MonolingualTextValue> labelsIfNew) {
		super(id, statements, labels, labelsIfNew);
    	Validate.isTrue(id instanceof MediaInfoIdValue, "the entity id must be an ItemIdValue");
	}
	
    /**
     * Protected constructor to avoid re-constructing term maps when
     * merging two entity updates.
     * 
     * No validation is done on the arguments, they all have to be non-null.
     * 
     * @param id
     * 		the subject of the update
     * @param addedStatements
     *      the statements to add
     * @param deletedStatements
     *      the statements to delete
     * @param labels
     *      the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     */
    protected MediaInfoEdit(
            EntityIdValue id,
    		List<StatementEdit> statements,
    		Map<String, MonolingualTextValue> labels,
    		Map<String, MonolingualTextValue> labelsIfNew) {
    	super(id, statements, labels, labelsIfNew);
    }

	@Override
	public MediaInfoUpdate toEntityUpdate(EntityDocument entityDocument) {
		MediaInfoDocument mediaInfoDocument = (MediaInfoDocument) entityDocument;
    	
    	// Labels (captions)
        List<MonolingualTextValue> labels = getLabels().stream().collect(Collectors.toList());
        labels.addAll(getLabelsIfNew().stream()
              .filter(label -> !mediaInfoDocument.getLabels().containsKey(label.getLanguageCode())).collect(Collectors.toList()));
        TermUpdate labelUpdate = Datamodel.makeTermUpdate(labels, Collections.emptyList());
        
        // Statements
        StatementUpdate statementUpdate = toStatementUpdate(mediaInfoDocument);
        
        return Datamodel.makeMediaInfoUpdate(
        		(MediaInfoIdValue) id,
                entityDocument.getRevisionId(),
                labelUpdate,
                statementUpdate);
	}

	@Override
	public MediaInfoEdit merge(EntityEdit otherEdit) {
        Validate.isTrue(id.equals(otherEdit.getEntityId()));
        Validate.isTrue(otherEdit instanceof MediaInfoEdit);
        MediaInfoEdit other = (MediaInfoEdit)otherEdit;
        List<StatementEdit> newStatements = new ArrayList<>(statements);
        for (StatementEdit statement : other.getStatementEdits()) {
            if (!newStatements.contains(statement)) {
                newStatements.add(statement);
            }
        }
        Map<String,MonolingualTextValue> newLabels = new HashMap<>(labels);
        Map<String,MonolingualTextValue> newLabelsIfNew = new HashMap<>(labelsIfNew);
        mergeSingleTermMaps(newLabels, newLabelsIfNew, other.getLabels(), other.getLabelsIfNew());
        return new MediaInfoEdit(id, newStatements, newLabels, newLabelsIfNew);
	}

	@Override
	public EntityDocument toNewEntity() {
		throw new NotImplementedException("Creating new entities of type mediainfo is not supported yet.");
	}

	@Override
	public boolean isEmpty() {
	    return (statements.isEmpty() &&
	    		labels.isEmpty() &&
	    		labelsIfNew.isEmpty());
	}
	
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<Update on ");
        builder.append(id);
        if (!labels.isEmpty()) {
            builder.append("\n  Labels (override): ");
            builder.append(labels);
        }
        if (!labelsIfNew.isEmpty()) {
            builder.append("\n  Labels (if new): ");
            builder.append(labelsIfNew);
        }
        if (!statements.isEmpty()) {
            builder.append("\n  Statements: ");
            builder.append(statements);
        }
        if (isNull()) {
            builder.append(" (null update)");
        }
        builder.append("\n>");
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null || !MediaInfoEdit.class.isInstance(other)) {
            return false;
        }
        MediaInfoEdit otherUpdate = (MediaInfoEdit) other;
        return id.equals(otherUpdate.getEntityId()) && statements.equals(otherUpdate.getStatementEdits())
                && getLabels().equals(otherUpdate.getLabels());
    }

    @Override
    public int hashCode() {
        return id.hashCode() + statements.hashCode() + labels.hashCode();
    }

}
