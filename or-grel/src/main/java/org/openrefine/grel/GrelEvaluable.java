package org.openrefine.grel;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openrefine.expr.Evaluable;
import org.openrefine.grel.ast.GrelExpr;

public class GrelEvaluable implements Evaluable {
    private static final long serialVersionUID = 916126073490453712L;
    private GrelExpr _expr;
    private String   _languagePrefix;

    public GrelEvaluable(GrelExpr expr, String languagePrefix) {
        _expr = expr;
        _languagePrefix = languagePrefix;
    }

    @Override
    public Object evaluate(Properties bindings) {
        return _expr.evaluate(bindings);
    }

    @Override
    public Set<String> getColumnDependencies(String baseColumn) {
        return _expr.getColumnDependencies(baseColumn);
    }
    
    @Override
    public Evaluable renameColumnDependencies(Map<String, String> substitutions) {
        GrelExpr newExpr = _expr.renameColumnDependencies(substitutions);
        return newExpr == null ? null : new GrelEvaluable(newExpr, _languagePrefix);
    }

    @Override
    public String getSource() {
        return _expr.toString();
    }

    @Override
    public String getLanguagePrefix() {
        return "grel";
    }
    
    @Override
    public boolean isLocal() {
        return _expr.isLocal();
    }
}