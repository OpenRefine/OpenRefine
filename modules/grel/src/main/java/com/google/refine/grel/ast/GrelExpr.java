
package com.google.refine.grel.ast;

import java.util.Map;
import java.util.Optional;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;

abstract class GrelExpr implements Evaluable {

    @Override
    public String getSource() {
        return toString();
    }

    @Override
    public String getLanguagePrefix() {
        return MetaParser.GREL_LANGUAGE_CODE;
    }

    // make sure all subclasses implement this method
    @Override
    public abstract Optional<Evaluable> renameColumnDependencies(Map<String, String> substitutions);
}
