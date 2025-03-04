
package org.openrefine.wikibase.schema;

import java.util.Map;
import java.util.Set;

/**
 * Base class for expressions which do not depend on any column.
 */
public abstract class WbConstantExpr<T> implements WbExpression<T> {

    @Override
    public WbExpression<T> renameColumns(Map<String, String> substitutions) {
        return this;
    }

    @Override
    public Set<String> getColumnDependencies() {
        return Set.of();
    }

}
