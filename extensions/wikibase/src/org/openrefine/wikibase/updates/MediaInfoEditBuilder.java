
package org.openrefine.wikibase.updates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

/**
 * Constructs a {@link MediaInfoEdit} incrementally.
 * 
 * @author Antonin Delpeuch
 *
 */
public class MediaInfoEditBuilder {

    private EntityIdValue id;
    private List<StatementEdit> statements;
    private Set<MonolingualTextValue> labels;
    private Set<MonolingualTextValue> labelsIfNew;
    private String filePath;
    private String fileName;
    private String wikitext;
    private boolean overrideWikitext;
    private boolean built;

    /**
     * Constructor.
     * 
     * @param id
     *            the subject of the document. It can be a reconciled entity value for new entities.
     */
    public MediaInfoEditBuilder(EntityIdValue id) {
        Validate.notNull(id);
        this.id = id;
        this.statements = new ArrayList<>();
        this.labels = new HashSet<MonolingualTextValue>();
        this.labelsIfNew = new HashSet<MonolingualTextValue>();
        this.filePath = null;
        this.fileName = null;
        this.wikitext = null;
        this.overrideWikitext = false;
        this.built = false;
    }

    /**
     * Adds an update to a statement.
     * 
     * @param statement
     *            the statement to add or update
     */
    public MediaInfoEditBuilder addStatement(StatementEdit statement) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        statements.add(statement);
        return this;
    }

    /**
     * Add a list of statement, as in {@link addStatement}.
     * 
     * @param statements
     *            the statements to add
     */
    public MediaInfoEditBuilder addStatements(List<StatementEdit> statements) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        statements.addAll(statements);
        return this;
    }

    /**
     * Adds a label to the entity.
     * 
     * @param label
     *            the label to add
     * @param override
     *            whether the label should be added even if there is already a label in that language
     */
    public MediaInfoEditBuilder addLabel(MonolingualTextValue label, boolean override) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        if (override) {
            labels.add(label);
        } else {
            labelsIfNew.add(label);
        }
        return this;
    }

    /**
     * Adds a list of labels to the entity.
     * 
     * @param labels
     *            the labels to add
     * @param override
     *            whether the label should be added even if there is already a label in that language
     */
    public MediaInfoEditBuilder addLabels(Set<MonolingualTextValue> labels, boolean override) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        if (override) {
            this.labels.addAll(labels);
        } else {
            labelsIfNew.addAll(labels);
        }
        return this;
    }

    /**
     * Sets the path of the file to be uploaded. Can be null for edits on existing entities.
     */
    public MediaInfoEditBuilder addFilePath(String path) {
        this.filePath = path;
        return this;
    }

    /**
     * Sets the file name to use when uploading (for new entities) or to move to (for existing entities).
     */
    public MediaInfoEditBuilder addFileName(String name) {
        this.fileName = name;
        return this;
    }

    /**
     * Sets the wikitext to be inserted in the edit (otherwise it is left as such)
     */
    public MediaInfoEditBuilder addWikitext(String wikitext) {
        this.wikitext = wikitext;
        return this;
    }

    /**
     * Sets whether the wikitext should override any existing one
     */
    public MediaInfoEditBuilder setOverrideWikitext(boolean overrideWikitext) {
        this.overrideWikitext = overrideWikitext;
        return this;
    }

    /**
     * Constructs the {@link MediaInfoEdit}.
     * 
     * @return
     */
    public MediaInfoEdit build() {
        built = true;
        return new MediaInfoEdit(id, statements, labels, labelsIfNew, filePath, fileName, wikitext, overrideWikitext);
    }
}
