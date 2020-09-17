package org.openrefine.wikidata.qa;

import org.openrefine.wikidata.schema.WbExpression;
import org.openrefine.wikidata.schema.WbItemDocumentExpr;
import org.openrefine.wikidata.schema.WbPropConstant;
import org.openrefine.wikidata.schema.WbReferenceExpr;
import org.openrefine.wikidata.schema.WbSnakExpr;
import org.openrefine.wikidata.schema.WbStatementExpr;
import org.openrefine.wikidata.schema.WbStatementGroupExpr;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SchemaPropertyExtractor {

    public Set<PropertyIdValue> getAllProperties(WikibaseSchema schema) {
        Set<PropertyIdValue> properties = new HashSet<>();
        List<WbItemDocumentExpr> itemDocumentExprs = schema.getItemDocumentExpressions();
        for (WbItemDocumentExpr itemDocumentExpr : itemDocumentExprs) {
            List<WbStatementGroupExpr> statementGroups = itemDocumentExpr.getStatementGroups();
            for(WbStatementGroupExpr statementGroup : statementGroups) {
                WbExpression<? extends PropertyIdValue> statementGroupProperty = statementGroup.getProperty();
                if (statementGroupProperty instanceof WbPropConstant) {
                    properties.add(Datamodel.makeWikidataPropertyIdValue(((WbPropConstant) statementGroupProperty).getPid()));
                }
                List<WbStatementExpr> statementExprs = statementGroup.getStatements();
                for(WbStatementExpr statementExpr : statementExprs) {
                    List<WbSnakExpr> snakExprs = new ArrayList<>(statementExpr.getQualifiers());
                    List<WbReferenceExpr> referenceExprs = statementExpr.getReferences();
                    for (WbReferenceExpr referenceExpr : referenceExprs) {
                        snakExprs.addAll(referenceExpr.getSnaks());
                    }

                    for (WbSnakExpr snakExpr : snakExprs) {
                        WbExpression<? extends PropertyIdValue> qualifierProperty = snakExpr.getProp();
                        if (qualifierProperty instanceof WbPropConstant) {
                            properties.add(Datamodel.makeWikidataPropertyIdValue(((WbPropConstant) qualifierProperty).getPid()));
                        }
                    }
                }
            }
        }

        return properties;
    }
}

