package org.openrefine.wikidata.schema;

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.entityvalues.SuggestedEntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A constant entity id value, that does not change depending on the row
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbEntityIdValueConstant implements WbExpression<EntityIdValue> {
	
    private String id;
    private String label;

    @JsonCreator
    public WbEntityIdValueConstant(
    		@JsonProperty("id") String id,
    		@JsonProperty("label") String label) {
        Validate.notNull(id, "id cannot be null");
        this.id = id;
        Validate.notNull(label, "label cannot be null");
        this.label = label;
    }

    @Override
    public EntityIdValue evaluate(ExpressionContext ctxt) {
        return SuggestedEntityIdValue.build(id, ctxt.getBaseIRI(), label);
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbEntityIdValueConstant.class.isInstance(other)) {
            return false;
        }
        WbEntityIdValueConstant otherConstant = (WbEntityIdValueConstant) other;
        return id.equals(otherConstant.getId()) && label.equals(otherConstant.getLabel());
    }

    @Override
    public int hashCode() {
        return id.hashCode() + label.hashCode();
    }
}
