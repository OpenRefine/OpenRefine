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

package org.openrefine.wikibase.updates.scheduler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.UnsupportedValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * A class that extracts the new entity ids referred to in a statement.
 * 
 * @author Antonin Delpeuch
 *
 */
public class PointerExtractor implements ValueVisitor<Set<ReconEntityIdValue>> {

    /**
     * Extracts all the new entities mentioned by this statement. This does not include the subject of the statement.
     * 
     * @param statement
     *            the statement to inspect
     * @return the set of all new entities mentioned by the statement
     */
    public Set<ReconEntityIdValue> extractPointers(Statement statement) {
        Set<ReconEntityIdValue> result = new HashSet<>();
        result.addAll(extractPointers(statement.getClaim().getMainSnak()));
        result.addAll(extractPointers(statement.getClaim().getQualifiers()));
        statement.getReferences().stream().map(l -> extractPointers(l.getSnakGroups())).forEach(s -> result.addAll(s));
        return result;
    }

    /**
     * Extracts all the new entities mentioned by this list of snak groups.
     * 
     * @param snakGroups
     * @return
     */
    public Set<ReconEntityIdValue> extractPointers(List<SnakGroup> snakGroups) {
        Set<ReconEntityIdValue> result = new HashSet<>();
        snakGroups.stream().map(s -> extractPointers(s)).forEach(s -> result.addAll(s));
        return result;
    }

    /***
     * Extracts all the new entities mentioned by this snak group.
     * 
     * @param snakGroup
     * @return
     */
    public Set<ReconEntityIdValue> extractPointers(SnakGroup snakGroup) {
        Set<ReconEntityIdValue> result = new HashSet<>();
        snakGroup.getSnaks().stream().map(s -> extractPointers(s)).forEach(s -> result.addAll(s));
        return result;
    }

    /**
     * Extracts all new entities mentioned by this snak group. Currently there will be at most one: the target of the
     * snak (as property ids cannot be new for now).
     * 
     * @param snak
     * @return
     */
    public Set<ReconEntityIdValue> extractPointers(Snak snak) {
        Set<ReconEntityIdValue> result = new HashSet<>();
        result.addAll(extractPointers(snak.getPropertyId()));
        if (snak instanceof ValueSnak) {
            result.addAll(extractPointers(((ValueSnak) snak).getValue()));
        }
        return result;
    }

    /**
     * Extracts any new entity from the value.
     * 
     * @param value
     * @return
     */
    public Set<ReconEntityIdValue> extractPointers(Value value) {
        if (value == null) {
            return Collections.emptySet();
        }
        Set<ReconEntityIdValue> pointers = value.accept(this);
        if (pointers == null) {
            return Collections.emptySet();
        }
        return pointers;
    }

    @Override
    public Set<ReconEntityIdValue> visit(EntityIdValue value) {
        if (ReconEntityIdValue.class.isInstance(value)) {
            ReconEntityIdValue recon = (ReconEntityIdValue) value;
            if (recon.isNew()) {
                return Collections.singleton(recon);
            }
        }
        return null;
    }

    @Override
    public Set<ReconEntityIdValue> visit(GlobeCoordinatesValue value) {
        return null;
    }

    @Override
    public Set<ReconEntityIdValue> visit(MonolingualTextValue value) {
        return null;
    }

    @Override
    public Set<ReconEntityIdValue> visit(QuantityValue value) {
        // units cannot be new because WDTK represents them as strings already
        return null;
    }

    @Override
    public Set<ReconEntityIdValue> visit(StringValue value) {
        return null;
    }

    @Override
    public Set<ReconEntityIdValue> visit(TimeValue value) {
        return null;
    }

    @Override
    public Set<ReconEntityIdValue> visit(UnsupportedValue value) {
        return null;
    }
}
