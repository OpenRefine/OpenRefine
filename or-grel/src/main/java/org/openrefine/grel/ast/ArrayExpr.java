
package org.openrefine.grel.ast;

import java.util.Map;

import org.openrefine.expr.functions.arrays.ArgsToArray;

public class ArrayExpr extends FunctionCallExpr {

    private static final long serialVersionUID = -6213937834177619498L;

    public ArrayExpr(GrelExpr[] args) {
        super(args, new ArgsToArray(), null);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (GrelExpr ev : _args) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ev.toString());
        }

        return "[ " + sb.toString() + " ]";
    }

    @Override
    public ArrayExpr renameColumnDependencies(Map<String, String> substitutions) {
        GrelExpr[] translatedArgs = new GrelExpr[_args.length];
        for (int i = 0; i != _args.length; i++) {
            translatedArgs[i] = _args[i].renameColumnDependencies(substitutions);
            if (translatedArgs[i] == null) {
                return null;
            }
        }
        return new ArrayExpr(translatedArgs);
    }
}
