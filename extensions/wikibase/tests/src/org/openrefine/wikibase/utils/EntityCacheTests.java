
package org.openrefine.wikibase.utils;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntityCacheTests {

    @Test
    public void testGet() throws MediaWikiApiErrorException, IOException {
        WikibaseDataFetcher fetcher = mock(WikibaseDataFetcher.class);
        PropertyIdValue id = Datamodel.makeWikidataPropertyIdValue("P42");
        PropertyDocument doc = Datamodel.makePropertyDocument(id, Datamodel.makeDatatypeIdValue(DatatypeIdValue.DT_GEO_SHAPE));
        when(fetcher.getEntityDocument(id.getId())).thenReturn(doc);

        EntityCache SUT = new EntityCache(fetcher);
        Assert.assertEquals(SUT.get(id), doc);
        // try another time, it is now cached
        Assert.assertEquals(SUT.get(id), doc);

        // the fetcher was only called once thanks to caching
        verify(fetcher, times(1)).getEntityDocument(id.getId());
    }

    @Test
    public void testGetAll() throws MediaWikiApiErrorException, IOException, ExecutionException {
        WikibaseDataFetcher fetcher = mock(WikibaseDataFetcher.class);
        PropertyIdValue idA = Datamodel.makeWikidataPropertyIdValue("P42");
        PropertyIdValue idB = Datamodel.makeWikidataPropertyIdValue("P43");
        PropertyIdValue idC = Datamodel.makeWikidataPropertyIdValue("P44");
        PropertyIdValue idD = Datamodel.makeWikidataPropertyIdValue("P45");

        PropertyDocument docA = Datamodel.makePropertyDocument(idA, Datamodel.makeDatatypeIdValue(DatatypeIdValue.DT_GEO_SHAPE));
        PropertyDocument docB = Datamodel.makePropertyDocument(idB, Datamodel.makeDatatypeIdValue(DatatypeIdValue.DT_GEO_SHAPE));
        PropertyDocument docC = Datamodel.makePropertyDocument(idC, Datamodel.makeDatatypeIdValue(DatatypeIdValue.DT_GEO_SHAPE));
        PropertyDocument docD = Datamodel.makePropertyDocument(idD, Datamodel.makeDatatypeIdValue(DatatypeIdValue.DT_GEO_SHAPE));

        EntityCache SUT = new EntityCache(fetcher);

        List<String> entityIdListA = Arrays.asList(idA.getId(), idB.getId());
        List<String> entityIdListB = Arrays.asList(idC.getId(), idD.getId());
        List<String> entityIdListC = Arrays.asList(idB.getId(), idC.getId());

        List<EntityDocument> docListA = Arrays.asList(docA, docB);
        List<EntityDocument> docListB = Arrays.asList(docC, docD);
        List<EntityDocument> docListC = Arrays.asList(docB, docC);

        Map<String, EntityDocument> docMapA = new HashMap<>();
        docMapA.put(idA.getId(), docA);
        docMapA.put(idB.getId(), docB);
        Map<String, EntityDocument> docMapB = new HashMap<>();
        docMapB.put(idC.getId(), docC);
        docMapB.put(idD.getId(), docD);
        Map<String, EntityDocument> docMapC = new HashMap<>();
        docMapC.put(idB.getId(), docB);
        docMapC.put(idC.getId(), docC);

        when(fetcher.getEntityDocuments(entityIdListA)).thenReturn(docMapA);
        when(fetcher.getEntityDocuments(entityIdListB)).thenReturn(docMapB);
        when(fetcher.getEntityDocuments(entityIdListC)).thenReturn(docMapC);

        Assert.assertEquals(SUT.getMultipleDocuments(Arrays.asList(idA, idB)), docListA);
        Assert.assertEquals(SUT.getMultipleDocuments(Arrays.asList(idC, idD)), docListB);
        Assert.assertEquals(SUT.getMultipleDocuments(Arrays.asList(idB, idC)), docListC);

        verify(fetcher, times(0)).getEntityDocuments(entityIdListC);
    }

}
