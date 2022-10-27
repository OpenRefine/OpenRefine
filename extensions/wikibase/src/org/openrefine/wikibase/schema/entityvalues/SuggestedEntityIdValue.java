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

package org.openrefine.wikibase.schema.entityvalues;

import java.util.ArrayList;
import java.util.List;

import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An EntityIdValue that we have obtained from a suggest widget in the schema alignment dialog.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class SuggestedEntityIdValue implements PrefetchedEntityIdValue {

    private String _id;
    private String _siteIRI;
    private String _label;

    public SuggestedEntityIdValue(String id, String siteIRI, String label) {
        _id = id;
        _siteIRI = siteIRI;
        _label = label;
    }

    public static SuggestedEntityIdValue build(String id, String siteIRI, String label) {
        String entityType = EntityIdValueImpl.guessEntityTypeFromId(id);
        if (DatatypeIdValue.DT_ITEM.equals(entityType)) {
            return new SuggestedItemIdValue(id, siteIRI, label);
        } else if (DatatypeIdValue.DT_PROPERTY.equals(entityType)) {
            return new SuggestedPropertyIdValue(id, siteIRI, label);
        } else if (DatatypeIdValue.DT_MEDIA_INFO.equals(entityType)) {
            return new SuggestedMediaInfoIdValue(id, siteIRI, label);
        } else if (DatatypeIdValue.DT_LEXEME.equals(entityType)) {
            return new SuggestedLexemeIdValue(id, siteIRI, label);
        } else if (DatatypeIdValue.DT_FORM.equals(entityType)) {
            return new SuggestedFormIdValue(id, siteIRI, label);
        } else if (DatatypeIdValue.DT_SENSE.equals(entityType)) {
            return new SuggestedSenseIdValue(id, siteIRI, label);
        } else {
            throw new IllegalArgumentException(
                    String.format("Unsupported datatype for suggested entity values: %s", entityType));
        }
    }

    @Override
    @JsonProperty("id")
    public String getId() {
        return _id;
    }

    @Override
    @JsonProperty("siteIri")
    public String getSiteIri() {
        return _siteIRI;
    }

    @Override
    @JsonProperty("label")
    public String getLabel() {
        return _label;
    }

    @Override
    @JsonProperty("types")
    public List<String> getTypes() {
        return new ArrayList<>();
    }

    @Override
    @JsonProperty("iri")
    public String getIri() {
        return getSiteIri() + getId();
    }

    @Override
    public boolean isPlaceholder() {
        return false;
    }

    @Override
    public <T> T accept(ValueVisitor<T> valueVisitor) {
        return valueVisitor.visit(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !EntityIdValue.class.isInstance(other)) {
            return false;
        }
        final EntityIdValue otherNew = (EntityIdValue) other;
        return getIri().equals(otherNew.getIri());
    }

    @Override
    public int hashCode() {
        return Hash.hashCode(this);
    }

    @Override
    public String toString() {
        return getIri();
    }

}
