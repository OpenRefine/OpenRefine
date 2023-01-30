/**
 * Supplies a {@link org.openrefine.model.Runner} which is designed for execution on a single machine, with parallelism.
 * Grids and change data objects are computed lazily, using the {@link PLL} data structure. This is the default runner
 * implementation.
 */

package org.openrefine.runners.local;
