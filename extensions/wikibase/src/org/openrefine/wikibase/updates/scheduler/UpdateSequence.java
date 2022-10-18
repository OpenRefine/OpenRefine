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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefine.wikibase.updates.EntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

/**
 * Helper class to store a list of updates where each subject appears at most once. It preserves order of insertion.
 * 
 * @author Antonin Delpeuch
 */
public class UpdateSequence {

    /**
     * The list of updates stored by this container
     */
    private List<EntityEdit> updates = new ArrayList<>();
    /**
     * An index to keep track of where each entity is touched in the sequence
     */
    private Map<EntityIdValue, Integer> index = new HashMap<>();

    /**
     * Adds a new update to the list, merging it with any existing one with the same subject.
     * 
     * @param update
     */
    public void add(EntityEdit update) {
        EntityIdValue subject = update.getEntityId();
        if (index.containsKey(subject)) {
            int i = index.get(subject);
            EntityEdit oldUpdate = updates.get(i);
            updates.set(i, oldUpdate.merge(update));
        } else {
            index.put(subject, updates.size());
            updates.add(update);
        }
    }

    /**
     * @return the list of merged updates
     */
    public List<EntityEdit> getUpdates() {
        return updates;
    }

    /**
     * @return the set of touched subjects
     */
    public Set<EntityIdValue> getSubjects() {
        return index.keySet();
    }
}
