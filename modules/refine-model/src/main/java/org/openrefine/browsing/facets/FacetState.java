
package org.openrefine.browsing.facets;

import java.io.Serializable;

/**
 * Immutable object which stores statistics gathered by a facet. It is required to be serializable by Spark as it is
 * sent back and forth to executors.
 *
 */
public interface FacetState extends Serializable {

}
