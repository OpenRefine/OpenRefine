package org.openrefine.wikidata.schema.exceptions;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

public class NewItemNotCreatedYetException extends Exception {
	private static final long serialVersionUID = -563535295696710197L;
	
	private final EntityIdValue value;

	public NewItemNotCreatedYetException(EntityIdValue value) {
		super("Attempted to rewrite an entity which was not created yet: "+value);
		this.value = value;
	}

	public EntityIdValue getMissingEntity() {
		return value;
	}

}
