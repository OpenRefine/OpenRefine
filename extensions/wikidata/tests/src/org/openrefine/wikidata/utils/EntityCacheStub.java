package org.openrefine.wikidata.utils;

import java.io.InputStream;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.DatamodelMapper;
import org.wikidata.wdtk.datamodel.implementation.EntityDocumentImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stub of EntityCache class, to fetch entities from a local cache instead
 * of the live site.
 * 
 * @author Antonin Delpeuch
 */
public class EntityCacheStub extends EntityCache {

	private ObjectMapper mapper =  new DatamodelMapper(Datamodel.SITE_WIKIDATA);
	
	public EntityCacheStub() {
		super(BasicApiConnection.getWikidataApiConnection());
	}
	
	@Override
	public EntityDocument get(EntityIdValue id) {
        String filename = "entitycache/entitycache-"+id.getId()+".json";
        InputStream stream = EntityCacheStub.class.getClassLoader().getResourceAsStream(filename);
        try {
        	// TODO This should ideally be hidden in a helper:
        	// https://github.com/Wikidata/Wikidata-Toolkit/issues/471
			return mapper.readValue(stream, EntityDocumentImpl.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }
}
