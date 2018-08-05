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
package org.openrefine.wikidata.updates.scheduler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * A class that extracts the new entity ids referred to in a statement.
 * 
 * @author Antonin Delpeuch
 *
 */
public class PointerExtractor implements ValueVisitor<Set<ReconItemIdValue>> {

    /**
     * Extracts all the new entities mentioned by this statement. This does not
     * include the subject of the statement.
     * 
     * @param statement
     *            the statement to inspect
     * @return the set of all new entities mentioned by the statement
     */
    public Set<ReconItemIdValue> extractPointers(Statement statement) {
        Set<ReconItemIdValue> result = new HashSet<>();
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
    public Set<ReconItemIdValue> extractPointers(List<SnakGroup> snakGroups) {
        Set<ReconItemIdValue> result = new HashSet<>();
        snakGroups.stream().map(s -> extractPointers(s)).forEach(s -> result.addAll(s));
        return result;
    }

    /***
     * Extracts all the new entities mentioned by this snak group.
     * 
     * @param snakGroup
     * @return
     */
    public Set<ReconItemIdValue> extractPointers(SnakGroup snakGroup) {
        Set<ReconItemIdValue> result = new HashSet<>();
        snakGroup.getSnaks().stream().map(s -> extractPointers(s)).forEach(s -> result.addAll(s));
        return result;
    }

    /**
     * Extracts all new entities mentioned by this snak group. Currently there will
     * be at most one: the target of the snak (as property ids cannot be new for
     * now).
     * 
     * @param snak
     * @return
     */
    public Set<ReconItemIdValue> extractPointers(Snak snak) {
        Set<ReconItemIdValue> result = new HashSet<>();
        result.addAll(extractPointers(snak.getPropertyId()));
        result.addAll(extractPointers(snak.getValue()));
        return result;
    }

    /**
     * Extracts any new entity from the value.
     * 
     * @param value
     * @return
     */
    public Set<ReconItemIdValue> extractPointers(Value value) {
        if (value == null) {
            return Collections.emptySet();
        }
        Set<ReconItemIdValue> pointers = value.accept(this);
        if (pointers == null) {
            return Collections.emptySet();
        }
        return pointers;
    }

    @Override
    public Set<ReconItemIdValue> visit(DatatypeIdValue value) {
        return null;
    }

    @Override
    public Set<ReconItemIdValue> visit(EntityIdValue value) {
        if (ReconItemIdValue.class.isInstance(value)) {
            ReconItemIdValue recon = (ReconItemIdValue) value;
            if (recon.isNew()) {
                return Collections.singleton(recon);
            }
        }
        return null;
    }

    @Override
    public Set<ReconItemIdValue> visit(GlobeCoordinatesValue value) {
        return null;
    }

    @Override
    public Set<ReconItemIdValue> visit(MonolingualTextValue value) {
        return null;
    }

    @Override
    public Set<ReconItemIdValue> visit(QuantityValue value) {
        // units cannot be new because WDTK represents them as strings already
        return null;
    }

    @Override
    public Set<ReconItemIdValue> visit(StringValue value) {
        return null;
    }

    @Override
    public Set<ReconItemIdValue> visit(TimeValue value) {
        return null;
    }
}
