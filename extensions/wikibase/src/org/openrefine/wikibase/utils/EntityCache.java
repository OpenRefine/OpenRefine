/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EntityCache {

    private static Map<String, EntityCache> entityCacheMap = new HashMap<>();

    private LoadingCache<String, EntityDocument> cache;

    protected EntityCache(String entityPrefix, String mediaWikiApiEndpoint) {
        this(new WikibaseDataFetcher(new BasicApiConnection(mediaWikiApiEndpoint), entityPrefix));
    }

    protected EntityCache(WikibaseDataFetcher fetcher) {
        cache = CacheBuilder.newBuilder().maximumSize(4096).expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, EntityDocument>() {

                    @Override
                    public EntityDocument load(String entityId)
                            throws Exception {
                        EntityDocument doc = fetcher.getEntityDocument(entityId);
                        if (doc != null) {
                            return doc;
                        } else {
                            throw new MediaWikiApiErrorException("400", "Unknown entity id \"" + entityId + "\"");
                        }
                    }

                    @Override
                    public Map<String, EntityDocument> loadAll(Iterable<? extends String> entityIds)
                            throws Exception {
                        Map<String, EntityDocument> entityDocumentMap = fetcher
                                .getEntityDocuments(StreamSupport.stream(entityIds.spliterator(), false)
                                        .collect(Collectors.toList()));
                        if (!entityDocumentMap.isEmpty()) {
                            return entityDocumentMap;
                        } else {
                            throw new MediaWikiApiErrorException("400", "Unknown entity ids in \"" + entityIds.toString() + "\"");
                        }
                    }

                });
    }

    public EntityDocument get(EntityIdValue id) {
        return cache.apply(id.getId());
    }

    /**
     * Get an entity cache for a given Wikibase instance.
     * 
     * @param siteIri
     * @param mediaWikiApiEndpoint
     * @return
     */
    public static EntityCache getEntityCache(String siteIri, String mediaWikiApiEndpoint) {
        EntityCache entityCache = entityCacheMap.get(siteIri);
        if (entityCache == null) {
            entityCache = new EntityCache(siteIri, mediaWikiApiEndpoint);
            entityCacheMap.put(siteIri, entityCache);
        }
        return entityCache;
    }

    /**
     * Provided for testability.
     * 
     * @param siteIri
     * @param cache
     */
    public static void setEntityCache(String siteIri, EntityCache cache) {
        entityCacheMap.put(siteIri, cache);
    }

    public List<EntityDocument> getMultipleDocuments(List<EntityIdValue> entityIds) throws ExecutionException {
        List<String> ids = entityIds.stream().map(entityId -> entityId.getId()).collect(Collectors.toList());
        return cache.getAll(ids).values().stream().collect(Collectors.toList());
    }

    public static EntityDocument getEntityDocument(String entityPrefix, String mediaWikiApiEndpoint, EntityIdValue id) {
        return getEntityCache(entityPrefix, mediaWikiApiEndpoint).get(id);
    }

    public static void removeEntityCache(String siteIri) {
        entityCacheMap.remove(siteIri);
    }
}
