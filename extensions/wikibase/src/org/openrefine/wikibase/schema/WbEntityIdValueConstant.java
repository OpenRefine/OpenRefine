
package org.openrefine.wikibase.schema;

import org.openrefine.wikibase.schema.entityvalues.SuggestedEntityIdValue;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
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
        this.id = id;
        this.label = label;
    }

    @Override
    public void validate(ValidationState validation) {
        if (id == null) {
            validation.addError("No entity id provided");
        } else {
            try {
                EntityIdValueImpl.guessEntityTypeFromId(id);
            } catch (IllegalArgumentException e) {
                validation.addError("Invalid entity id format: '" + id + "'");
            }
        }
        if (label == null) {
            validation.addError("No entity label provided");
        }
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
