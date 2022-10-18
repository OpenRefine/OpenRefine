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
import java.util.Arrays;
import java.util.List;

import org.wikidata.wdtk.datamodel.helpers.Equality;
import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.model.Recon;

/**
 * An EntityIdValue that holds not just the id but also the label as fetched by either the reconciliation interface or
 * the suggester and its type, both stored as reconciliation candidates.
 * 
 * This label will be localized depending on the language chosen by the user for OpenRefine's interface. Storing it lets
 * us reuse it later on without having to re-fetch it.
 * 
 * Storing the types also lets us perform some constraint checks without re-fetching the types of many entities.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ReconEntityIdValue implements PrefetchedEntityIdValue {

    private Recon _recon;
    private String _cellValue;

    public ReconEntityIdValue(Recon match, String cellValue) {
        _recon = match;
        _cellValue = cellValue;
        assert (Recon.Judgment.Matched.equals(_recon.judgment) || Recon.Judgment.New.equals(_recon.judgment));
    }

    @JsonIgnore
    public boolean isMatched() {
        return Recon.Judgment.Matched.equals(_recon.judgment) && _recon.match != null;
    }

    @JsonIgnore
    public boolean isNew() {
        return !isMatched();
    }

    @JsonProperty("label")
    public String getLabel() {
        if (isMatched()) {
            return _recon.match.name;
        } else {
            return _cellValue;
        }
    }

    @JsonProperty("types")
    public List<String> getTypes() {
        if (isMatched()) {
            return Arrays.asList(_recon.match.types);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    @JsonProperty("entityType")
    public abstract String getEntityType();

    /**
     * Returns the integer used internally in OpenRefine to identify the new entity.
     * 
     * @return the reconciliation id of the reconciled cell
     */
    @JsonProperty("reconInternalId")
    public long getReconInternalId() {
        return getRecon().id;
    }

    /**
     * Returns the reconciliation object corresponding to this entity.
     * 
     * @return the full reconciliation metadata of the corresponding cell
     */
    @JsonIgnore // just to clean up a bit the json serialization
    public Recon getRecon() {
        return _recon;
    }

    /**
     * Returns the id of the reconciled entity
     */
    @Override
    @JsonProperty("id")
    public String getId() {
        if (isMatched()) {
            return _recon.match.id;
        } else if (ET_ITEM.equals(getEntityType())) {
            return "Q" + getReconInternalId();
        } else if (ET_PROPERTY.equals(getEntityType())) {
            return "P" + getReconInternalId();
        } else if (ET_MEDIA_INFO.equals(getEntityType())) {
            return "M" + getReconInternalId();
        } else {
            throw new IllegalStateException("Unsupported entity type: " + getEntityType());
        }
    }

    @Override
    @JsonProperty("siteIri")
    public String getSiteIri() {
        if (isMatched()) {
            return _recon.identifierSpace;
        } else {
            return EntityIdValue.SITE_LOCAL;
        }
    }

    @Override
    @JsonProperty("iri")
    public String getIri() {
        return getSiteIri() + getId();
    }

    @Override
    public <T> T accept(ValueVisitor<T> valueVisitor) {
        return valueVisitor.visit(this);
    }

    @Override
    public boolean equals(Object other) {
        return Equality.equalsEntityIdValue(this, other);

    }

    @Override
    public int hashCode() {
        return Hash.hashCode(this);
    }

    @Override
    public String toString() {
        if (isNew()) {
            return "new item (reconciled from " + getReconInternalId() + ")";
        } else {
            return getIri() + " (reconciled from " + getReconInternalId() + ")";
        }
    }
}
