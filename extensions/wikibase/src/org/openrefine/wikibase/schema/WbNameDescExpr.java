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

package org.openrefine.wikibase.schema;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.PathElement;
import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An expression that represent a term (label, description or alias). The structure is slightly different from other
 * expressions because we need to call different methods on {@link ItemEditBuilder}.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WbNameDescExpr {

    enum NameDescType {
        LABEL, LABEL_IF_NEW, DESCRIPTION, DESCRIPTION_IF_NEW, ALIAS,
    }

    private NameDescType type;
    private WbMonolingualExpr value;

    @JsonCreator
    public WbNameDescExpr(@JsonProperty("name_type") NameDescType type,
            @JsonProperty("value") WbMonolingualExpr value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Checks that the expression has all its required elements and can be evaluated.
     */
    public void validate(ValidationState validation) {
        if (type == null) {
            validation.addError("Missing type");
        }
        if (value == null) {
            validation.addError("Missing value");
        } else {
            validation.enter();
            value.validate(validation);
            validation.leave();
        }
    }

    /**
     * Evaluates the expression and adds the result to the entity update.
     * 
     * @param entity
     *            the entity update where the term should be stored
     * @param ctxt
     *            the evaluation context for the expression
     * @throws QAWarningException
     */
    public void contributeTo(ItemEditBuilder entity, ExpressionContext ctxt) throws QAWarningException {
        try {
            MonolingualTextValue val = getValue().evaluate(ctxt);
            switch (getType()) {
                case LABEL:
                    entity.addLabel(val, true);
                    break;
                case LABEL_IF_NEW:
                    entity.addLabel(val, false);
                    break;
                case DESCRIPTION:
                    entity.addDescription(val, true);
                    break;
                case DESCRIPTION_IF_NEW:
                    entity.addDescription(val, false);
                    break;
                case ALIAS:
                    entity.addAlias(val);
                    break;
            }
        } catch (SkipSchemaExpressionException e) {
            return;
        }
    }

    /**
     * Evaluates the expression and adds the result to the entity update.
     * 
     * @param entity
     *            the entity update where the term should be stored
     * @param ctxt
     *            the evaluation context for the expression
     * @throws QAWarningException
     */
    public void contributeTo(MediaInfoEditBuilder entity, ExpressionContext ctxt) throws QAWarningException {
        try {
            MonolingualTextValue val = getValue().evaluate(ctxt);
            switch (getType()) {
                case LABEL:
                    entity.addLabel(val, true);
                    break;
                case LABEL_IF_NEW:
                    entity.addLabel(val, false);
                    break;
                default:
                    throw new IllegalArgumentException("Term type not supported by MediaInfo entities");
            }
        } catch (SkipSchemaExpressionException e) {
            return;
        }
    }

    @JsonProperty("name_type")
    public NameDescType getType() {
        return type;
    }

    @JsonProperty("value")
    public WbMonolingualExpr getValue() {
        return value;
    }

    // for error-reporting purposes, during schema validation
    @JsonIgnore
    public PathElement.Type getPathElementType() {
        switch (getType()) {
            case ALIAS:
                return Type.ALIAS;
            case DESCRIPTION:
                return Type.DESCRIPTION;
            case DESCRIPTION_IF_NEW:
                return Type.DESCRIPTION;
            case LABEL:
                return Type.LABEL;
            case LABEL_IF_NEW:
                return Type.LABEL;
        }
        throw new IllegalStateException("Non-exhaustive enumeration of term types");
    }

    /**
     * For error-reporting purposes, during schema validation.
     * 
     * @return the constant language code for this term, if there is one, otherwise null
     */
    @JsonIgnore
    public String getStaticLanguage() {
        if (value != null && value.getLanguageExpr() != null && value.getLanguageExpr() instanceof WbLanguageConstant) {
            return ((WbLanguageConstant) value.getLanguageExpr()).getLabel();
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbNameDescExpr.class.isInstance(other)) {
            return false;
        }
        WbNameDescExpr otherExpr = (WbNameDescExpr) other;
        return type.equals(otherExpr.getType()) && value.equals(otherExpr.getValue());
    }

    @Override
    public int hashCode() {
        return type.hashCode() + value.hashCode();
    }

}
