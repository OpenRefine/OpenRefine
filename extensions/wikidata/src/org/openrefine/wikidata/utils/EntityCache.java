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
package org.openrefine.wikidata.utils;

import java.util.concurrent.TimeUnit;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EntityCache {

    private static EntityCache _entityCache = new EntityCache(BasicApiConnection.getWikidataApiConnection());

    private LoadingCache<String, EntityDocument> _cache = null;
    private WikibaseDataFetcher _fetcher;

    protected EntityCache(ApiConnection connection) {
        this(new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA));
    }
    
    protected EntityCache(WikibaseDataFetcher fetcher) {
        _fetcher = fetcher;

        _cache = CacheBuilder.newBuilder().maximumSize(4096).expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, EntityDocument>() {

                    public EntityDocument load(String entityId)
                            throws Exception {
                        EntityDocument doc = _fetcher.getEntityDocument(entityId);
                        if (doc != null) {
                            return doc;
                        } else {
                            throw new MediaWikiApiErrorException("400", "Unknown entity id \"" + entityId + "\"");
                        }
                    }
                });
    }

    public EntityDocument get(EntityIdValue id) {
        return _cache.apply(id.getId());
    }

    public static EntityCache getEntityCache() {
        if (_entityCache == null) {
            _entityCache = new EntityCache(BasicApiConnection.getWikidataApiConnection());
        }
        return _entityCache;
    }

    public static EntityDocument getEntityDocument(EntityIdValue id) {
        return getEntityCache().get(id);
    }
}
