package org.openrefine.wikidata.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

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
}
