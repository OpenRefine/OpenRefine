
package org.openrefine.wikibase.updates;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.jsoup.helper.Validate;
import org.openrefine.wikibase.editing.MediaFileUtils;
import org.openrefine.wikibase.editing.NewEntityLibrary;
import org.openrefine.wikibase.editing.ReconEntityRewriter;
import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.schema.exceptions.NewEntityNotCreatedYetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoDocument;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoUpdate;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a candidate edit on a MediaInfo entity.
 * 
 * @author Antonin Delpeuch
 *
 */
public class MediaInfoEdit extends LabeledStatementEntityEdit {

    protected final String filePath;
    protected final String fileName;
    protected final String wikitext;

    static final Logger logger = LoggerFactory.getLogger(MediaInfoEdit.class);
    protected final boolean overrideWikitext;

    /**
     * Constructor.
     * 
     * @param id
     *            the subject of the document. It can be a reconciled entity value for new entities.
     * @param statements
     *            the statements to change on the entity.
     * @param labels
     *            the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     * @param filePath
     *            the path of the file to upload
     * @param fileName
     *            the desired file name on the wiki (File:…)
     * @param wikitext
     *            the wikitext to associate to the file (depending on overriding settings)
     * @param overrideWikitext
     *            whether the supplied wikitext should override any existing one
     */
    public MediaInfoEdit(
            EntityIdValue id,
            List<StatementEdit> statements,
            Set<MonolingualTextValue> labels,
            Set<MonolingualTextValue> labelsIfNew,
            String filePath,
            String fileName,
            String wikitext,
            boolean overrideWikitext) {
        super(id, statements, labels, labelsIfNew);
        this.filePath = filePath;
        this.fileName = fileName;
        this.wikitext = wikitext;
        this.overrideWikitext = overrideWikitext;
    }

    /**
     * Protected constructor to avoid re-constructing term maps when merging two entity updates.
     * 
     * No validation is done on the arguments, they all have to be non-null.
     * 
     * @param id
     *            the subject of the update
     * @param statements
     *            the statements to edit
     * @param labels
     *            the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     * @param filePath
     *            the path of the file to upload
     * @param fileName
     *            the desired file name on the wiki (File:…)
     * @param wikitext
     *            the wikitext to associate to the file (depending on overriding settings)
     * @param overrideWikitext
     *            whether the supplied wikitext should override any existing one
     */
    protected MediaInfoEdit(
            EntityIdValue id,
            List<StatementEdit> statements,
            Map<String, MonolingualTextValue> labels,
            Map<String, MonolingualTextValue> labelsIfNew,
            String filePath,
            String fileName,
            String wikitext,
            boolean overrideWikitext) {
        super(id, statements, labels, labelsIfNew);
        this.filePath = filePath;
        this.fileName = fileName;
        this.wikitext = wikitext;
        this.overrideWikitext = overrideWikitext;
    }

    @JsonProperty("filePath")
    public String getFilePath() {
        return filePath;
    }

    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    @JsonProperty("wikitext")
    public String getWikitext() {
        return wikitext;
    }

    @JsonProperty("overrideWikitext")
    public boolean isOverridingWikitext() {
        return overrideWikitext;
    }

    @Override
    public FullMediaInfoUpdate toEntityUpdate(EntityDocument entityDocument) {
        if (entityDocument == null) {
            Validate.isFalse(requiresFetchingExistingState(), "No existing entity state provided");
            return new FullMediaInfoUpdate(
                    (MediaInfoIdValue) id,
                    0L,
                    Datamodel.makeTermUpdate(Collections.emptyList(), Collections.emptyList()),
                    Datamodel.makeStatementUpdate(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                    filePath,
                    fileName,
                    wikitext,
                    overrideWikitext);
        }
        MediaInfoDocument mediaInfoDocument = (MediaInfoDocument) entityDocument;

        // Labels (captions)
        List<MonolingualTextValue> labels = getLabels().stream().collect(Collectors.toList());
        labels.addAll(getLabelsIfNew().stream()
                .filter(label -> !mediaInfoDocument.getLabels().containsKey(label.getLanguageCode())).collect(Collectors.toList()));
        TermUpdate labelUpdate = Datamodel.makeTermUpdate(labels, Collections.emptyList());

        // Statements
        StatementUpdate statementUpdate = toStatementUpdate(mediaInfoDocument);

        return new FullMediaInfoUpdate(
                // important: use the id from the document, not from the update, as
                // they might not be the same if a redirect has happened
                mediaInfoDocument.getEntityId(),
                entityDocument.getRevisionId(),
                labelUpdate,
                statementUpdate,
                filePath,
                fileName,
                wikitext,
                overrideWikitext);
    }

    @Override
    public MediaInfoEdit merge(EntityEdit otherEdit) {
        Validate.isTrue(id.equals(otherEdit.getEntityId()));
        Validate.isTrue(otherEdit instanceof MediaInfoEdit);
        MediaInfoEdit other = (MediaInfoEdit) otherEdit;
        List<StatementEdit> newStatements = new ArrayList<>(statements);
        for (StatementEdit statement : other.getStatementEdits()) {
            if (!newStatements.contains(statement)) {
                newStatements.add(statement);
            }
        }
        Map<String, MonolingualTextValue> newLabels = new HashMap<>(labels);
        Map<String, MonolingualTextValue> newLabelsIfNew = new HashMap<>(labelsIfNew);
        mergeSingleTermMaps(newLabels, newLabelsIfNew, other.getLabels(), other.getLabelsIfNew());
        String newFilePath = other.getFilePath() == null ? filePath : other.getFilePath();
        String newFileName = other.getFileName() == null ? fileName : other.getFileName();
        String newWikitext = other.getWikitext() == null ? wikitext : other.getWikitext();
        if (overrideWikitext && wikitext != null && !other.isOverridingWikitext()) {
            newWikitext = wikitext;
        }
        boolean newOverrideWikitext = other.isOverridingWikitext() || overrideWikitext;
        return new MediaInfoEdit(id, newStatements, newLabels, newLabelsIfNew, newFilePath, newFileName, newWikitext, newOverrideWikitext);
    }

    @Override
    public EntityDocument toNewEntity() {
        throw new NotImplementedException("Creating new entities of type mediainfo is not supported yet.");
    }

    /**
     * If the update corresponds to a new file, uploads the new file, its wikitext and its metadata.
     * 
     * @param editor
     *            the {@link WikibaseDataEditor} to use
     * @param mediaFileUtils
     *            the {@link MediaFileUtils} to use
     * @param summary
     *            the edit summary
     * @param tags
     *            the tags to apply to both edits
     * @return the id of the created entity
     * @throws MediaWikiApiErrorException
     * @throws IOException
     */
    public MediaInfoIdValue uploadNewFile(WikibaseDataEditor editor, MediaFileUtils mediaFileUtils, String summary, List<String> tags)
            throws MediaWikiApiErrorException, IOException {
        Validate.isTrue(isNew());
        // Temporary addition of the category (should be configurable)
        String wikitext = this.wikitext;
        if (!wikitext.contains("[[Category:Uploaded with OpenRefine]]")) {
            wikitext = wikitext + "\n[[Category:Uploaded with OpenRefine]]";
        }

        // Upload the file
        MediaFileUtils.MediaUploadResponse response;
        File path = new File(filePath);
        if (path.exists()) {
            response = mediaFileUtils.uploadLocalFile(path, fileName, wikitext, summary, tags);
        } else {
            URL url = new URL(filePath);
            response = mediaFileUtils.uploadRemoteFile(url, fileName, wikitext, summary, tags);
        }

        response.checkForErrors();

        // Upload the structured data
        ReconEntityIdValue reconEntityIdValue = (ReconEntityIdValue) id;
        MediaInfoIdValue mid = response.getMid(mediaFileUtils.getApiConnection(), reconEntityIdValue.getRecon().identifierSpace);
        NewEntityLibrary library = new NewEntityLibrary();
        library.setId(reconEntityIdValue.getReconInternalId(), mid.getId());
        ReconEntityRewriter rewriter = new ReconEntityRewriter(library, id);
        try {
            MediaInfoEdit rewritten = (MediaInfoEdit) rewriter.rewrite(this);
            MediaInfoUpdate update = rewritten.toEntityUpdate(Datamodel.makeMediaInfoDocument(mid));
            editor.editEntityDocument(update, false, summary, tags);
        } catch (NewEntityNotCreatedYetException e) {
            // should not be reachable as the scheduling should have been done before
            Validate.fail("The entity edit contains references to new entities which have not been created yet.");
        }

        // perform a null edit to trigger an update of rendered wikitext (for instance, for up to date categories)
        // https://phabricator.wikimedia.org/T237991
        try {
            mediaFileUtils.purgePage(Long.parseLong(mid.getId().substring(1)));
        } catch (MediaWikiApiErrorException e) {
            // if we failed to purge but still managed to carry out all the earlier steps,
            // we want to return the created mid, so catching this exception here
            logger.warn("Failed to purge page after structured data edit:");
            logger.warn(e.getErrorMessage());
        }
        return mid;
    }

    @Override
    public boolean isEmpty() {
        return (statements.isEmpty() &&
                labels.isEmpty() &&
                labelsIfNew.isEmpty() &&
                filePath == null &&
                fileName == null &&
                wikitext == null);
    }

    @Override
    public boolean requiresFetchingExistingState() {
        /*
         * If all the Wikibase-related fields are empty, then we can skip fetching the current entity from the wiki.
         * This makes it possible to use the wikitext editing feature for Wikibases which do not use MediaInfo.
         */
        return !isNew() && !(statements.isEmpty() &&
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
        if (filePath != null) {
            builder.append("\n  File path: ");
            builder.append(filePath);
        }
        if (fileName != null) {
            builder.append("\n File name: ");
            builder.append(fileName);
        }
        if (fileName != null) {
            builder.append(overrideWikitext ? "\n Wikitext (overrride): " : "\n Wikitext (no override): ");
            builder.append(wikitext);
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
        return id.equals(otherUpdate.getEntityId())
                && statements.equals(otherUpdate.getStatementEdits())
                && getLabels().equals(otherUpdate.getLabels())
                && Objects.equals(filePath, otherUpdate.getFilePath())
                && Objects.equals(fileName, otherUpdate.getFileName())
                && Objects.equals(wikitext, otherUpdate.getWikitext())
                && overrideWikitext == otherUpdate.isOverridingWikitext();
    }

    @Override
    public int hashCode() {
        return id.hashCode() + statements.hashCode() + labels.hashCode();
    }

}
