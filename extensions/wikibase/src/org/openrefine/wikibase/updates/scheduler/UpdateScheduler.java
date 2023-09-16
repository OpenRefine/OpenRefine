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

import java.util.List;

import org.openrefine.wikibase.updates.EntityEdit;

/**
 * A scheduling strategy for entity updates. Given a list of initial updates, the scheduler reorganizes these updates
 * (possibly splitting them or merging them) to create a sequence that is suitable for a particular import process.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface UpdateScheduler {

    /**
     * Performs the scheduling. The initial updates are provided as a list so that the scheduler can attempt to respect
     * the initial order (but no guarantee is made for that in general).
     * 
     * @param updates
     *            the updates to schedule
     * @return the reorganized updates
     * @throws ImpossibleSchedulingException
     *             when the scheduler cannot cope with a particular edit plan.
     */
    public List<EntityEdit> schedule(List<EntityEdit> updates)
            throws ImpossibleSchedulingException;
}
