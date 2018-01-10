package org.openrefine.wikidata.utils;

import java.util.concurrent.TimeUnit;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EntityCache {
    private static EntityCache _entityCache = new EntityCache();
    
    private LoadingCache<String, EntityDocument> _cache = null;
    private WikibaseDataFetcher _fetcher;
    
    private EntityCache() {
        ApiConnection connection = ApiConnection.getWikidataApiConnection();
        _fetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);

        System.out.println("Creating fresh cache");
        _cache = CacheBuilder.newBuilder()
                .maximumSize(4096)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
             new CacheLoader<String, EntityDocument>() {
                 public EntityDocument load(String entityId) throws Exception {
                     EntityDocument doc = _fetcher.getEntityDocument(entityId);
                     if (doc != null) {
                         return doc;
                     } else {
                         throw new MediaWikiApiErrorException("400", "Unknown entity id \""+entityId+"\"");
                     }
                 }
             });
    }
    
    public EntityDocument get(EntityIdValue id) {
        return _cache.apply(id.getId());
    }
    
    public static EntityCache getEntityCache() {
        if (_entityCache == null) {
            _entityCache = new EntityCache();
        }
        return _entityCache;
    }
    
    public static EntityDocument getEntityDocument(EntityIdValue id) {
        return getEntityCache().get(id);
    }
}
