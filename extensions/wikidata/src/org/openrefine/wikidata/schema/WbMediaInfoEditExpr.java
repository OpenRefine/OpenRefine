package org.openrefine.wikidata.schema;

import java.util.Collections;
import java.util.List;

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarning.Severity;
import org.openrefine.wikidata.schema.WbNameDescExpr.NameDescType;
import org.openrefine.wikidata.schema.exceptions.QAWarningException;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.updates.MediaInfoEdit;
import org.openrefine.wikidata.updates.MediaInfoEditBuilder;
import org.openrefine.wikidata.updates.StatementEdit;
import org.openrefine.wikidata.updates.StatementGroupEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The representation of an item edit, which can contain variables both for
 * its own id and in its contents.
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
    
    public static final String INVALID_SUBJECT_WARNING_TYPE = "invalid-mediainfo-subject";
    
    @JsonCreator
    public WbMediaInfoEditExpr(
    		@JsonProperty("subject") WbExpression<? extends EntityIdValue> subjectExpr,
            @JsonProperty("nameDescs") List<WbNameDescExpr> nameDescExprs,
            @JsonProperty("statementGroups") List<WbStatementGroupExpr> statementGroupExprs,
            @JsonProperty("filePath") WbExpression<StringValue> filePath,
            @JsonProperty("fileName") WbExpression<StringValue> fileName,
            @JsonProperty("wikitext") WbExpression<StringValue> wikitext) {
        Validate.notNull(subjectExpr);
        this.subject = subjectExpr;
        if (nameDescExprs == null) {
            nameDescExprs = Collections.emptyList();
        }
        nameDescExprs.stream().forEach(nde ->
        	Validate.isTrue(nde.getType() == NameDescType.LABEL || nde.getType() == NameDescType.LABEL_IF_NEW));
        this.nameDescs = nameDescExprs;
        if (statementGroupExprs == null) {
            statementGroupExprs = Collections.emptyList();
        }
        this.statementGroups = statementGroupExprs;
        this.filePath = filePath;
        this.fileName = fileName;
        this.wikitext = wikitext;
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
        	} catch(SkipSchemaExpressionException e) {
        		;
        	}
        }
        if (fileName != null) {
        	try {
        		StringValue nameValue = fileName.evaluate(ctxt);
        		if (nameValue != null && !nameValue.getString().isBlank()) {
        			update.addFileName(nameValue.getString());
        		}
        	} catch(SkipSchemaExpressionException e) {
        		;
        	}
        }
        if (wikitext != null) {
        	try {
        		StringValue wikitextValue = wikitext.evaluate(ctxt);
        		if (wikitextValue != null && !wikitextValue.getString().isBlank()) {
        			update.addWikitext(wikitextValue.getString());
        		}
        	} catch(SkipSchemaExpressionException e) {
        		;
        	}
        }
        return update.build();
	}
	
	/**
	 * The Mid for the entity. If marked as new, a new
	 * file will be uploaded, otherwise the existing MediaInfo
	 * entity will be edited.
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
     * The path to the file that should be uploaded,
     * either to replace the current file if the subject already exists,
     * or to create a new file on the MediaWiki instance if the subject
     * does not exist yet.
     * 
     * Can be null if editing existing entities only (in which case the files' contents are not changed)
     */
    @JsonProperty("filePath")
    public WbExpression<StringValue> getFilePath() {
    	return filePath;
    }
    
    /**
     * The filename at which the file should be uploaded (if new),
     * or moved (if existing).
     * 
     * Can be null if editing existing entities only (in which case the files will not be moved)
     */
    @JsonProperty("fileName")
    public WbExpression<StringValue> getFileName() {
    	return fileName;
    }
    
    /**
     * The wikitext which replaces any existing wikitext.
     * @return
     */
    @JsonProperty("wikitext")
    public WbExpression<StringValue> getWikitext() {
    	return wikitext;
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
