package org.openrefine.wikidata.updates.scheduler;

import java.util.List;

import org.openrefine.wikidata.updates.ItemUpdate;

/**
 * A scheduling strategy for item updates.
 * Given a list of initial updates, the scheduler
 * reorganizes these updates (possibly splitting them
 * or merging them) to create a sequence that is suitable
 * for a particular import process.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface UpdateScheduler {
    
    /**
     * Performs the scheduling. The initial updates are provided
     * as a list so that the scheduler can attempt to respect the
     * initial order (but no guarantee is made for that in general).
     * 
     * @param updates
     *      the updates to schedule
     * @return
     *      the reorganized updates
     * @throws ImpossibleSchedulingException 
     *      when the scheduler cannot cope with a particular edit plan.
     */
    public List<ItemUpdate> schedule(List<ItemUpdate> updates) throws ImpossibleSchedulingException;
}
