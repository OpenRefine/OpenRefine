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

package org.openrefine.wikidata.editing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.testing.WikidataRefineTest;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.openrefine.wikidata.updates.TermedStatementEntityEditBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoDocument;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class EditBatchProcessorTest extends WikidataRefineTest {

    private WikibaseDataFetcher fetcher = null;
    private WikibaseDataEditor editor = null;
    private NewEntityLibrary library = null;
    private String summary = "my fantastic edits";
    private int maxlag = 5;
    private List<String> tags = null;

    @BeforeMethod
    public void setUp() {
        fetcher = mock(WikibaseDataFetcher.class);
        editor = mock(WikibaseDataEditor.class);
        editor.disableEditing(); // just in case we got mocking wrong…
        library = new NewEntityLibrary();// new entities created in the test
        tags = Arrays.asList("my-tag");
    }

    @Test
    public void testNewItem()
            throws InterruptedException, MediaWikiApiErrorException, IOException {
        List<TermedStatementEntityEdit> batch = new ArrayList<>();
        batch.add(new TermedStatementEntityEditBuilder(TestingData.existingId)
                .addAlias(Datamodel.makeMonolingualTextValue("my new alias", "en"))
                .addStatement(TestingData.generateStatementAddition(TestingData.existingId, TestingData.newIdA)).build());
        MonolingualTextValue label = Datamodel.makeMonolingualTextValue("better label", "en");
        batch.add(new TermedStatementEntityEditBuilder(TestingData.newIdA).addAlias(label).build());

        // Plan expected edits
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr")).build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        ItemDocument expectedNewItem = ItemDocumentBuilder.forItemId(TestingData.newIdA).withLabel(label).build();
        ItemDocument createdNewItem = ItemDocumentBuilder.forItemId(Datamodel.makeWikidataItemIdValue("Q1234"))
                .withLabel(label).withRevisionId(37828L).build();
        when(editor.createItemDocument(expectedNewItem, summary, tags)).thenReturn(createdNewItem);

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, batch, library, summary, maxlag, tags, 50, 60);
        assertEquals(2, processor.remainingEdits());
        assertEquals(0, processor.progress());
        processor.performEdit();
        assertEquals(1, processor.remainingEdits());
        assertEquals(50, processor.progress());
        processor.performEdit();
        assertEquals(0, processor.remainingEdits());
        assertEquals(100, processor.progress());
        processor.performEdit(); // does not do anything
        assertEquals(0, processor.remainingEdits());
        assertEquals(100, processor.progress());

        NewEntityLibrary expectedLibrary = new NewEntityLibrary();
        expectedLibrary.setId(1234L, "Q1234");
        assertEquals(expectedLibrary, library);
    }

    @Test
    public void testMultipleBatches()
            throws MediaWikiApiErrorException, InterruptedException, IOException {
        // Prepare test data
        MonolingualTextValue description = Datamodel.makeMonolingualTextValue("village in Nepal", "en");
        List<String> ids = new ArrayList<>();
        for (int i = 124; i < 190; i++) {
            ids.add("Q" + String.valueOf(i));
        }
        List<ItemIdValue> qids = ids.stream().map(e -> Datamodel.makeWikidataItemIdValue(e))
                .collect(Collectors.toList());
        List<TermedStatementEntityEdit> batch = qids.stream()
                .map(qid -> new TermedStatementEntityEditBuilder(qid).addDescription(description, true).build())
                .collect(Collectors.toList());

        int batchSize = 50;
        List<ItemDocument> fullBatch = qids.stream()
                .map(qid -> ItemDocumentBuilder.forItemId(qid)
                        .withStatement(TestingData.generateStatement(qid, TestingData.existingId)).build())
                .collect(Collectors.toList());
        List<ItemDocument> firstBatch = fullBatch.subList(0, batchSize);
        List<ItemDocument> secondBatch = fullBatch.subList(batchSize, fullBatch.size());

        when(fetcher.getEntityDocuments(toQids(firstBatch))).thenReturn(toMap(firstBatch));
        when(fetcher.getEntityDocuments(toQids(secondBatch))).thenReturn(toMap(secondBatch));

        // Run edits
        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, batch, library, summary, maxlag, tags, batchSize, 60);
        assertEquals(0, processor.progress());
        for (int i = 124; i < 190; i++) {
            assertEquals(processor.remainingEdits(), 190 - i);
            processor.performEdit();
        }
        assertEquals(0, processor.remainingEdits());
        assertEquals(100, processor.progress());

        // Check result
        assertEquals(new NewEntityLibrary(), library);
        verify(fetcher, times(1)).getEntityDocuments(toQids(firstBatch));
        verify(fetcher, times(1)).getEntityDocuments(toQids(secondBatch));
        for (ItemDocument doc : fullBatch) {
            verify(editor, times(1)).editEntityDocument(Datamodel.makeItemUpdate(doc.getEntityId(),
                    doc.getRevisionId(), Datamodel.makeTermUpdate(Collections.emptyList(), Collections.emptyList()),
                    Datamodel.makeTermUpdate(Collections.singletonList(description), Collections.emptyList()),
                    Collections.emptyMap(),
                    Datamodel.makeStatementUpdate(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                    Collections.emptyList(), Collections.emptyList()), false, summary, tags);
        }
    }

    @Test
    public void testMultipleBatchesMediaInfo()
            throws MediaWikiApiErrorException, InterruptedException, IOException {
        // Prepare test data
        MonolingualTextValue label = Datamodel.makeMonolingualTextValue("village in Nepal", "en");
        List<MonolingualTextValue> labels = Collections.singletonList(label);
        TermUpdate labelsUpdate = Datamodel.makeTermUpdate(labels, Collections.emptyList());
        List<String> ids = new ArrayList<>();
        for (int i = 124; i < 190; i++) {
            ids.add("M" + String.valueOf(i));
        }
        List<MediaInfoIdValue> mids = ids.stream().map(e -> Datamodel.makeWikimediaCommonsMediaInfoIdValue(e))
                .collect(Collectors.toList());
        List<TermedStatementEntityEdit> batch = mids.stream()
                .map(mid -> new TermedStatementEntityEditBuilder(mid).addLabel(label, false).build())
                .collect(Collectors.toList());

        int batchSize = 50;
        List<MediaInfoDocument> fullBatch = mids.stream()
                .map(mid -> Datamodel.makeMediaInfoDocument(mid)).collect(Collectors.toList());
        List<MediaInfoDocument> firstBatch = fullBatch.subList(0, batchSize);
        List<MediaInfoDocument> secondBatch = fullBatch.subList(batchSize, fullBatch.size());

        when(fetcher.getEntityDocuments(toMids(firstBatch))).thenReturn(toMapMediaInfo(firstBatch));
        when(fetcher.getEntityDocuments(toMids(secondBatch))).thenReturn(toMapMediaInfo(secondBatch));

        // Run edits
        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, batch, library, summary, maxlag, tags, batchSize, 60);
        assertEquals(0, processor.progress());
        for (int i = 124; i < 190; i++) {
            assertEquals(processor.remainingEdits(), 190 - i);
            processor.performEdit();
        }
        assertEquals(0, processor.remainingEdits());
        assertEquals(100, processor.progress());

        // Check result
        assertEquals(new NewEntityLibrary(), library);
        verify(fetcher, times(1)).getEntityDocuments(toMids(firstBatch));
        verify(fetcher, times(1)).getEntityDocuments(toMids(secondBatch));
        for (MediaInfoDocument doc : fullBatch) {
            StatementUpdate statementUpdate = Datamodel.makeStatementUpdate(Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());
            verify(editor, times(1)).editEntityDocument(Datamodel.makeMediaInfoUpdate((MediaInfoIdValue) doc.getEntityId(),
                    doc.getRevisionId(), labelsUpdate, statementUpdate), false, summary, tags);
        }
    }

    private Map<String, EntityDocument> toMap(List<ItemDocument> docs) {
        return docs.stream().collect(Collectors.toMap(doc -> doc.getEntityId().getId(), doc -> doc));
    }

    private List<String> toQids(List<ItemDocument> docs) {
        return docs.stream().map(doc -> doc.getEntityId().getId()).collect(Collectors.toList());
    }

    private Map<String, EntityDocument> toMapMediaInfo(List<MediaInfoDocument> docs) {
        return docs.stream().collect(Collectors.toMap(doc -> doc.getEntityId().getId(), doc -> doc));
    }

    private List<String> toMids(List<MediaInfoDocument> firstBatch) {
        return firstBatch.stream().map(doc -> doc.getEntityId().getId()).collect(Collectors.toList());
    }
}
