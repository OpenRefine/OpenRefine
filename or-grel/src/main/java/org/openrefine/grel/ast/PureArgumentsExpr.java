
package org.openrefine.grel.ast;

import java.util.HashSet;
import java.util.Set;

import org.openrefine.expr.Evaluable;

/**
 * An abstract class for an expression whose column dependencies are the union of those of their arguments.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class PureArgumentsExpr implements Evaluable {

    private static final long serialVersionUID = 3199617968479062898L;
    protected final Evaluable[] _args;

    public PureArgumentsExpr(Evaluable[] arguments) {
        _args = arguments;
    }

    @Override
    public final Set<String> getColumnDependencies(String baseColumn) {
        Set<String> dependencies = new HashSet<>();
        for (Evaluable ev : _args) {
            Set<String> deps = ev.getColumnDependencies(baseColumn);
            if (deps == null) {
                return null;
            }
            dependencies.addAll(deps);
        }
        return dependencies;
    }

}
