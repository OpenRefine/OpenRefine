/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.schema;

import java.util.Collections;
import java.util.List;

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.updates.ItemEdit;
import org.openrefine.wikidata.updates.ItemEditBuilder;
import org.openrefine.wikidata.updates.StatementEdit;
import org.openrefine.wikidata.updates.StatementGroupEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

/**
 * The representation of an item edit, which can contain variables both for
 * its own id and in its contents.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = WbItemEditExpr.class, property = "type")
@JsonSubTypes({
    @Type(value = WbItemEditExpr.class, name = "wbitemeditexpr"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class WbItemEditExpr implements WbExpression<ItemEdit> {

    private WbExpression<? extends EntityIdValue> subject;
    private List<WbNameDescExpr> nameDescs;
    private List<WbStatementGroupExpr> statementGroups;

    @JsonCreator
    public WbItemEditExpr(
    		@JsonProperty("subject") WbExpression<? extends EntityIdValue> subjectExpr,
            @JsonProperty("nameDescs") List<WbNameDescExpr> nameDescExprs,
            @JsonProperty("statementGroups") List<WbStatementGroupExpr> statementGroupExprs) {
        Validate.notNull(subjectExpr);
        this.subject = subjectExpr;
        if (nameDescExprs == null) {
            nameDescExprs = Collections.emptyList();
        }
        this.nameDescs = nameDescExprs;
        if (statementGroupExprs == null) {
            statementGroupExprs = Collections.emptyList();
        }
        this.statementGroups = statementGroupExprs;
    }

    @Override
    public ItemEdit evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        EntityIdValue subjectId = getSubject().evaluate(ctxt);
        ItemEditBuilder update = new ItemEditBuilder(subjectId);
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
        if (other == null || !WbItemEditExpr.class.isInstance(other)) {
            return false;
        }
        WbItemEditExpr otherExpr = (WbItemEditExpr) other;
        return subject.equals(otherExpr.getSubject()) && nameDescs.equals(otherExpr.getNameDescs())
                && statementGroups.equals(otherExpr.getStatementGroups());
    }

    @Override
    public int hashCode() {
        return subject.hashCode() + nameDescs.hashCode() + statementGroups.hashCode();
    }
}
