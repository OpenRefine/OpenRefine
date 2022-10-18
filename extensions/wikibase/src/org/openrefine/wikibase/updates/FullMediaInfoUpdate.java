
package org.openrefine.wikibase.updates;

import org.apache.commons.lang.Validate;
import org.wikidata.wdtk.datamodel.implementation.MediaInfoUpdateImpl;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoUpdate;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An extension of WDTK's {@link MediaInfoUpdate} which also lets us change the file name of a file and/or replace its
 * contents.
 * 
 * @author Antonin Delpeuch
 *
 */
public class FullMediaInfoUpdate extends MediaInfoUpdateImpl implements MediaInfoUpdate {

    private final String filePath;
    private final String fileName;
    private final String wikitext;
    private final boolean overrideWikitext;

    /**
     * Creates an update on an existing mediainfo entity.
     * 
     * @param entityId
     *            the MediaInfo id of the entity to update
     * @param baseRevisionId
     *            the expected current revision id of the entity to edit.
     * @param labels
     *            the edits on captions
     * @param statementUpdate
     *            the edits on statements
     * @param filePath
     *            if not null, the path or URL to the file to use to replace the current contents of the file
     * @param fileName
     *            if not null, the new on-wiki filename to move the file to
     */
    public FullMediaInfoUpdate(
            MediaInfoIdValue entityId,
            long baseRevisionId,
            TermUpdate labels,
            StatementUpdate statementUpdate,
            String filePath,
            String fileName,
            String wikitext,
            boolean overrideWikitext) {
        super(entityId, baseRevisionId, labels, statementUpdate);
        Validate.notNull(entityId);
        this.filePath = filePath;
        this.fileName = fileName;
        this.wikitext = wikitext;
        this.overrideWikitext = overrideWikitext;
        Validate.notNull(labels);
        Validate.notNull(statementUpdate);
    }

    @Override
    public boolean isEmpty() {
        // intentionally ignoring our custom fields filePath, fileName and wikitext,
        // because we want to preserve the meaning of isEmpty to only cover the wikibase part of
        // the update.
        return super.isEmpty();
    }

    /**
     * If not null, the path or URL to the file which should replace the media contents of this entity. If null, the
     * contents of the entity are left untouched.
     */
    @JsonIgnore
    public String getFilePath() {
        return filePath;
    }

    /**
     * If not null, the new on-wiki filename associated with the entity. If null, the filename is left untouched.
     */
    @JsonIgnore
    public String getFileName() {
        return fileName;
    }

    /**
     * The wikitext which should replace any existing wikitext on the page. If null, the wikitext is left untouched.
     * 
     * @return
     */
    @JsonIgnore
    public String getWikitext() {
        return wikitext;
    }

    /**
     * Whether any existing wikitext should be overridden
     */
    @JsonIgnore
    public boolean isOverridingWikitext() {
        return overrideWikitext;
    }
}
