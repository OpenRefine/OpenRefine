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

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An expression that represent a term (label, description or alias). The
 * structure is slightly different from other expressions because we need to
 * call different methods on {@link ItemUpdateBuilder}.
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
        Validate.notNull(type);
        this.type = type;
        Validate.notNull(value);
        this.value = value;
    }

    /**
     * Evaluates the expression and adds the result to the item update.
     * 
     * @param item
     *            the item update where the term should be stored
     * @param ctxt
     *            the evaluation context for the expression
     */
    public void contributeTo(ItemUpdateBuilder item, ExpressionContext ctxt) {
        try {
            MonolingualTextValue val = getValue().evaluate(ctxt);
            switch (getType()) {
            case LABEL:
                item.addLabel(val, true);
                break;
            case LABEL_IF_NEW:
            	item.addLabel(val, false);
            	break;
            case DESCRIPTION:
                item.addDescription(val, true);
                break;
            case DESCRIPTION_IF_NEW:
            	item.addDescription(val, false);
            	break;
            case ALIAS:
                item.addAlias(val);
                break;
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
