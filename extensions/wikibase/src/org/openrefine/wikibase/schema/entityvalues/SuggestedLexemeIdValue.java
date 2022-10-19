
package org.openrefine.wikibase.schema.entityvalues;

import org.wikidata.wdtk.datamodel.interfaces.LexemeIdValue;

public class SuggestedLexemeIdValue extends SuggestedEntityIdValue implements LexemeIdValue {

    public SuggestedLexemeIdValue(String id, String siteIRI, String label) {
        super(id, siteIRI, label);
    }

    @Override
    public String getEntityType() {
        return ET_LEXEME;
    }
}
