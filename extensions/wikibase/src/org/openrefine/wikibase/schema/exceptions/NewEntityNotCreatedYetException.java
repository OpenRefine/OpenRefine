
package org.openrefine.wikibase.schema.exceptions;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

public class NewEntityNotCreatedYetException extends Exception {

    private static final long serialVersionUID = -563535295696710197L;

    private final EntityIdValue value;

    public NewEntityNotCreatedYetException(EntityIdValue value) {
        super("Attempted to rewrite an entity which was not created yet: " + value);
        this.value = value;
    }

    public EntityIdValue getMissingEntity() {
        return value;
    }

}
