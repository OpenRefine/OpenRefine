package org.openrefine.wikidata.schema;

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An expression that represent a term (label, description or alias).
 * The structure is slightly different from other expressions because
 * we need to call different methods on {@link ItemUpdateBuilder}.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WbNameDescExpr {
    
    enum NameDescrType {
        LABEL,
        DESCRIPTION,
        ALIAS,
    }
    
    private NameDescrType type;
    private WbMonolingualExpr value;
   
    @JsonCreator
    public WbNameDescExpr(
            @JsonProperty("name_type") NameDescrType type,
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
     *      the item update where the term should be stored
     * @param ctxt
     *       the evaluation context for the expression
     */
    public void contributeTo(ItemUpdateBuilder item, ExpressionContext ctxt) {
        try {
            MonolingualTextValue val = getValue().evaluate(ctxt);
            switch (getType()) {
                case LABEL:
                    item.addLabel(val);
                    break;
                case DESCRIPTION:
                    item.addDescription(val);
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
    public NameDescrType getType() {
        return type;
    }

    @JsonProperty("value")
    public WbMonolingualExpr getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !WbNameDescExpr.class.isInstance(other)) {
            return false;
        }
        WbNameDescExpr otherExpr = (WbNameDescExpr)other;
        return type.equals(otherExpr.getType()) &&
                value.equals(otherExpr.getValue());
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() + value.hashCode();
    }
}
