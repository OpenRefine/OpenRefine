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
    
    public static final String INVALID_SUBJECT_WARNING_TYPE = "invalid-mediainfo-subject";
    
    @JsonCreator
    public WbMediaInfoEditExpr(
    		@JsonProperty("subject") WbExpression<? extends EntityIdValue> subjectExpr,
            @JsonProperty("nameDescs") List<WbNameDescExpr> nameDescExprs,
            @JsonProperty("statementGroups") List<WbStatementGroupExpr> statementGroupExprs) {
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
    }


	@Override
	public MediaInfoEdit evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException, QAWarningException {
        EntityIdValue subjectId = getSubject().evaluate(ctxt);
        if (!(subjectId instanceof MediaInfoIdValue)) {
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
        return update.build();
	}
	
    @JsonProperty("subject")
    public WbExpression<? extends EntityIdValue> getSubject() {
        return subject;
    }

    @JsonProperty("nameDescs")
    public List<WbNameDescExpr> getNameDescs() {
        return Collections.unmodifiableList(nameDescs);
    }

    @JsonProperty("statementGroups")
    public List<WbStatementGroupExpr> getStatementGroups() {
        return Collections.unmodifiableList(statementGroups);
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
