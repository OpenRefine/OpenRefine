
package org.openrefine.wikibase.schema;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarning.Severity;
import org.openrefine.wikibase.schema.WbNameDescExpr.NameDescType;
import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.PathElement;
import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.StatementGroupEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The representation of an item edit, which can contain variables both for its own id and in its contents.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbMediaInfoEditExpr implements WbExpression<MediaInfoEdit> {

    private WbExpression<? extends EntityIdValue> subject;
    private List<WbNameDescExpr> nameDescs;
    private List<WbStatementGroupExpr> statementGroups;
    private WbExpression<StringValue> filePath;
    private WbExpression<StringValue> fileName;
    private WbExpression<StringValue> wikitext;
    private boolean overrideWikitext;

    public static final Pattern normalizedFileNameChars = Pattern.compile("[:/\\\\]");

    public static final String INVALID_SUBJECT_WARNING_TYPE = "invalid-mediainfo-subject";
    public static final String REPLACED_CHARACTERS_IN_FILENAME = "replaced-characters-in-filename";

    @JsonCreator
    public WbMediaInfoEditExpr(
            @JsonProperty("subject") WbExpression<? extends EntityIdValue> subjectExpr,
            @JsonProperty("nameDescs") List<WbNameDescExpr> nameDescExprs,
            @JsonProperty("statementGroups") List<WbStatementGroupExpr> statementGroupExprs,
            @JsonProperty("filePath") WbExpression<StringValue> filePath,
            @JsonProperty("fileName") WbExpression<StringValue> fileName,
            @JsonProperty("wikitext") WbExpression<StringValue> wikitext,
            @JsonProperty("overrideWikitext") boolean overrideWikitext) {
        this.subject = subjectExpr;
        if (nameDescExprs == null) {
            nameDescExprs = Collections.emptyList();
        }
        this.nameDescs = nameDescExprs;
        if (statementGroupExprs == null) {
            statementGroupExprs = Collections.emptyList();
        }
        this.statementGroups = statementGroupExprs == null ? Collections.emptyList() : statementGroupExprs;
        this.filePath = filePath;
        this.fileName = fileName;
        this.wikitext = wikitext;
        this.overrideWikitext = overrideWikitext;
    }

    @Override
    public void validate(ValidationState validation) {
        if (subject == null) {
            validation.addError("No subject provided");
        } else {
            validation.enter(new PathElement(Type.SUBJECT));
            subject.validate(validation);
            validation.leave();
        }
        nameDescs.stream().forEach(nde -> {
            if (nde == null) {
                validation.addError("Null term in MediaInfo entity");
            } else if (!(nde.getType() == NameDescType.LABEL || nde.getType() == NameDescType.LABEL_IF_NEW)) {
                validation.addError("Invalid term type for MediaInfo entity: " + nde.getType());
            } else {
                validation.enter(new PathElement(nde.getPathElementType(), nde.getStaticLanguage()));
                nde.validate(validation);
                validation.leave();
            }
        });
        statementGroups.stream().forEach(statementGroup -> {
            if (statementGroup == null) {
                validation.addError("Null statement in MediaInfo entity");
            } else {
                statementGroup.validate(validation);
            }
        });
        if (fileName != null) {
            validation.enter(new PathElement(Type.FILENAME));
            fileName.validate(validation);
            validation.leave();
        }
        if (filePath != null) {
            validation.enter(new PathElement(Type.FILEPATH));
            filePath.validate(validation);
            validation.leave();
        }
        if (wikitext != null) {
            validation.enter(new PathElement(Type.WIKITEXT));
            wikitext.validate(validation);
            validation.leave();
        }
    }

    @Override
    public MediaInfoEdit evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException, QAWarningException {
        EntityIdValue subjectId = getSubject().evaluate(ctxt);
        if (!(subjectId instanceof MediaInfoIdValue) && !subjectId.isPlaceholder()) {
            QAWarning warning = new QAWarning(INVALID_SUBJECT_WARNING_TYPE, "", Severity.CRITICAL, 1);
            warning.setProperty("example", subjectId.getId());
            throw new QAWarningException(warning);
        }
        MediaInfoEditBuilder update = new MediaInfoEditBuilder(subjectId);
        for (WbStatementGroupExpr expr : getStatementGroups()) {
            try {
                StatementGroupEdit statementGroupUpdate = expr.evaluate(ctxt, subjectId);
                for (StatementEdit s : statementGroupUpdate.getStatementEdits()) {
                    update.addStatement(s);
                }
            } catch (SkipSchemaExpressionException e) {
                continue;
            }
        }
        for (WbNameDescExpr expr : getNameDescs()) {
            expr.contributeTo(update, ctxt);
        }
        if (filePath != null) {
            try {
                StringValue pathValue = filePath.evaluate(ctxt);
                if (pathValue != null && !pathValue.getString().isBlank()) {
                    update.addFilePath(pathValue.getString());
                }
            } catch (SkipSchemaExpressionException e) {
                ;
            }
        }
        if (fileName != null) {
            try {
                StringValue nameValue = fileName.evaluate(ctxt);
                if (nameValue != null && !nameValue.getString().isBlank()) {
                    String fileName = nameValue.getString();
                    if (normalizedFileNameChars.matcher(fileName).find()) {
                        fileName = fileName.replaceAll(normalizedFileNameChars.pattern(), "-");
                        QAWarning warning = new QAWarning(REPLACED_CHARACTERS_IN_FILENAME, null, Severity.INFO, 1);
                        warning.setProperty("original", nameValue.getString());
                        warning.setProperty("normalized", fileName);
                        ctxt.addWarning(warning);
                    }
                    update.addFileName(fileName);
                }
            } catch (SkipSchemaExpressionException e) {
                ;
            }
        }
        if (wikitext != null) {
            try {
                StringValue wikitextValue = wikitext.evaluate(ctxt);
                if (wikitextValue != null && !wikitextValue.getString().isBlank()) {
                    update.addWikitext(wikitextValue.getString());
                }
            } catch (SkipSchemaExpressionException e) {
                ;
            }
        }
        if (overrideWikitext) {
            update.setOverrideWikitext(overrideWikitext);
        }
        return update.build();
    }

    /**
     * The Mid for the entity. If marked as new, a new file will be uploaded, otherwise the existing MediaInfo entity
     * will be edited.
     */
    @JsonProperty("subject")
    public WbExpression<? extends EntityIdValue> getSubject() {
        return subject;
    }

    /**
     * The captions edited on the mediainfo entity.
     */
    @JsonProperty("nameDescs")
    public List<WbNameDescExpr> getNameDescs() {
        return Collections.unmodifiableList(nameDescs);
    }

    /**
     * The statements edited on the mediainfo entity.
     */
    @JsonProperty("statementGroups")
    public List<WbStatementGroupExpr> getStatementGroups() {
        return Collections.unmodifiableList(statementGroups);
    }

    /**
     * The path to the file that should be uploaded, either to replace the current file if the subject already exists,
     * or to create a new file on the MediaWiki instance if the subject does not exist yet.
     * 
     * Can be null if editing existing entities only (in which case the files' contents are not changed)
     */
    @JsonProperty("filePath")
    public WbExpression<StringValue> getFilePath() {
        return filePath;
    }

    /**
     * The filename at which the file should be uploaded (if new), or moved (if existing).
     * 
     * Can be null if editing existing entities only (in which case the files will not be moved)
     */
    @JsonProperty("fileName")
    public WbExpression<StringValue> getFileName() {
        return fileName;
    }

    /**
     * The wikitext to be added to the file (or edited)
     * 
     * @return
     */
    @JsonProperty("wikitext")
    public WbExpression<StringValue> getWikitext() {
        return wikitext;
    }

    /**
     * Whether the provided wikitext should override any existing wikitext.
     */
    @JsonProperty("overrideWikitext")
    public boolean isOverridingWikitext() {
        return overrideWikitext;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbMediaInfoEditExpr.class.isInstance(other)) {
            return false;
        }
        WbMediaInfoEditExpr otherExpr = (WbMediaInfoEditExpr) other;
        return subject.equals(otherExpr.getSubject()) && nameDescs.equals(otherExpr.getNameDescs())
                && statementGroups.equals(otherExpr.getStatementGroups());
    }

    @Override
    public int hashCode() {
        return subject.hashCode() + nameDescs.hashCode() + statementGroups.hashCode();
    }

}
