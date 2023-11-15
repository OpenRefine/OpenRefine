
package org.openrefine.runners.local.pll.util;

/**
 * Encapsulates some contextual data which is passed to a {@link org.openrefine.runners.local.pll.PLL} when iterating
 * from it. This is useful for passing information about which parts of the elements must be computed (enabling columnar
 * reading of tables) or whether the iteration should be synchronous with another data source which concurrently fills
 * the collection with elements being iterated on.
 */
public interface IterationContext {

    /**
     * The default iteration context, used when carrying out most operations on a grid (such as displaying it), which
     * can accomodate with incomplete values.
     */
    public static final IterationContext DEFAULT = new IterationContext() {

    };

    /**
     * The default synchronous iteration context, used when storing a ChangeData object or caching a Grid.
     */
    public static final IterationContext SYNCHRONOUS_DEFAULT = new IterationContext() {

        @Override
        public boolean generateIncompleteElements() {
            return false;
        }
    };

    /**
     * When iterating over a collection that is being computed by another process, there are two options:
     * <ul>
     * <li>if an element is not fully computed yet, wait for the other process to compute it before enumerating it
     * ourselves</li>
     * <li>emit a placeholder (or a partially computed version of the element) so that we can carry on to iterate on the
     * rest of the collection. This is the default behaviour.</li>
     * </ul>
     *
     * The return value of this method is set to true whenever the second behaviour is desired.
     */
    default boolean generateIncompleteElements() {
        return true;
    }
}
