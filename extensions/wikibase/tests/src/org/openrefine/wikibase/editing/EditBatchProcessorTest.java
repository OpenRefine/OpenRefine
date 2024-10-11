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

package org.openrefine.wikibase.editing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.EditingResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiErrorMessage;

import com.google.refine.util.ParsingUtilities;

import org.openrefine.wikibase.editing.EditBatchProcessor.EditResult;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.testing.WikidataRefineTest;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;

public class EditBatchProcessorTest extends WikidataRefineTest {

    private WikibaseDataFetcher fetcher = null;
    private WikibaseDataEditor editor = null;
    private ApiConnection connection = null;
    private NewEntityLibrary library = null;
    private String summary = "my fantastic edits";
    private int maxlag = 5;
    private List<String> tags = null;

    private static final String successfulEditResponse = "{\n"
            + "    \"edit\": {\n"
            + "        \"result\": \"Success\",\n"
            + "        \"pageid\": 94542,\n"
            + "        \"title\": \"File:My_test_file.png\",\n"
            + "        \"contentmodel\": \"wikitext\",\n"
            + "        \"oldrevid\": 371705,\n"
            + "        \"newrevid\": 371707,\n"
            + "        \"newtimestamp\": \"2018-12-18T16:59:42Z\"\n"
            + "    }\n"
            + "}";

    @BeforeMethod
    public void setUp() {
        fetcher = mock(WikibaseDataFetcher.class);
        editor = mock(WikibaseDataEditor.class);
        connection = mock(ApiConnection.class);
        library = new NewEntityLibrary();// new entities created in the test
        tags = Arrays.asList("my-tag");
    }

    @Test
    public void testNewItem()
            throws InterruptedException, MediaWikiApiErrorException, IOException {
        List<EntityEdit> batch = new ArrayList<>();
        MonolingualTextValue alias = Datamodel.makeMonolingualTextValue("my new alias", "en");
        StatementEdit statement = TestingData.generateStatementAddition(TestingData.existingId, TestingData.newIdA);
        batch.add(new ItemEditBuilder(TestingData.existingId)
                .addAlias(alias)
                .addStatement(statement)
                .addContributingRowId(123).build());
        MonolingualTextValue label = Datamodel.makeMonolingualTextValue("better label", "en");
        batch.add(new ItemEditBuilder(TestingData.newIdA).addAlias(label).addContributingRowId(123).build());

        // Plan expected edits
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr"))
                .build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        ItemDocument expectedNewItem = ItemDocumentBuilder.forItemId(TestingData.newIdA).withLabel(label).build();
        ItemDocument createdNewItem = ItemDocumentBuilder.forItemId(Datamodel.makeWikidataItemIdValue("Q1234"))
                .withLabel(label).withRevisionId(37828L).build();
        when(editor.createEntityDocument(expectedNewItem, summary, tags)).thenReturn(createdNewItem);
        when(editor.editEntityDocument(any(), eq(false), eq(summary), eq(tags))).thenReturn(new EditingResult(18439L));

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag, tags, 50, 60);
        assertEquals(2, processor.remainingEdits());
        assertEquals(0, processor.progress());
        processor.performEdit();
        assertEquals(1, processor.remainingEdits());
        assertEquals(50, processor.progress());
        processor.performEdit();
        assertEquals(0, processor.remainingEdits());
        assertEquals(100, processor.progress());

        NewEntityLibrary expectedLibrary = new NewEntityLibrary();
        expectedLibrary.setId(1234L, "Q1234");
        assertEquals(expectedLibrary, library);
    }

    @Test
    public void testEmptyEdit()
            throws InterruptedException, MediaWikiApiErrorException, IOException {
        List<EntityEdit> batch = new ArrayList<>();
        batch.add(new ItemEditBuilder(TestingData.existingId).addContributingRowId(123).build());

        // Plan expected edits
        when(fetcher.getEntityDocuments(Collections.emptyList()))
                .thenReturn(Collections.emptyMap());

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag, tags, 50, 60);
        assertEquals(1, processor.remainingEdits());
        assertEquals(0, processor.progress());
        processor.performEdit();
        assertEquals(0, processor.remainingEdits());
        assertEquals(100, processor.progress());
    }

    @Test
    public void testFallbackToSecondTag() throws Exception {
        List<EntityEdit> batch = new ArrayList<>();
        batch.add(new ItemEditBuilder(TestingData.existingId)
                .addAlias(Datamodel.makeMonolingualTextValue("my new alias", "en"))
                .addContributingRowId(123)
                .build());
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr")).build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag"))))
                .thenThrow(new MediaWikiApiErrorException("badtags", "The tag 'first-tag' cannot be manually applied"));
        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("second-tag"))))
                .thenReturn(new EditingResult(12345L));

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag,
                Arrays.asList("first-tag", "second-tag"), 50, 60);
        assertEquals(1, processor.remainingEdits());
        processor.performEdit();
        assertEquals(0, processor.remainingEdits());

        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag")));
        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("second-tag")));
    }

    @Test
    public void testFallbackToNoTag() throws Exception {
        List<EntityEdit> batch = new ArrayList<>();
        batch.add(new ItemEditBuilder(TestingData.existingId)
                .addAlias(Datamodel.makeMonolingualTextValue("my new alias", "en"))
                .addContributingRowId(123)
                .build());
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr")).build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag"))))
                .thenThrow(new MediaWikiApiErrorException("badtags", "The tag 'first-tag' cannot be manually applied"));
        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.emptyList())))
                .thenReturn(new EditingResult(12345L));

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag,
                Arrays.asList("first-tag"), 50, 60);
        assertEquals(1, processor.remainingEdits());
        processor.performEdit();
        assertEquals(0, processor.remainingEdits());

        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag")));
        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.emptyList()));
    }

    @Test
    public void testDeletedItem() throws IOException, MediaWikiApiErrorException, InterruptedException {
        String id = "Q389";
        ItemIdValue qid = Datamodel.makeWikidataItemIdValue(id);
        MonolingualTextValue description = Datamodel.makeMonolingualTextValue("village in Nepal", "en");
        EntityEdit edit = new ItemEditBuilder(qid)
                .addDescription(description, true)
                .addContributingRowId(123)
                .build();
        List<EntityEdit> batch = Collections.singletonList(edit);
        when(fetcher.getEntityDocuments(Collections.singletonList(id))).thenReturn(Collections.emptyMap());

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library,
                summary, maxlag, tags, 10, 60);
        assertEquals(processor.progress(), 0);
        processor.performEdit();
        assertEquals(processor.progress(), 100);
        verify(editor, times(0)).editEntityDocument(any(), anyBoolean(), any(), any());
    }

    @Test
    public void testSaveHasFailedError() throws Exception {
        List<EntityEdit> batch = new ArrayList<>();
        batch.add(new ItemEditBuilder(TestingData.existingId)
                .addAlias(Datamodel.makeMonolingualTextValue("see https://tinyurl.com/some_url", "en"))
                .addContributingRowId(123)
                .build());
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr")).build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag"))))
                .thenThrow(new MediaWikiApiErrorException("failed-save", "The save has failed.",
                        Arrays.asList(new MediaWikiErrorMessage("wikibase-api-failed-save", "The save has failed."),
                                new MediaWikiErrorMessage("spam-blacklisted-link",
                                        "The text you wanted to publish was blocked by the spam filter."))));

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag,
                Arrays.asList("first-tag", "second-tag"), 50, 60);
        assertEquals(1, processor.remainingEdits());
        EditResult editResult = processor.performEdit();
        assertEquals(0, processor.remainingEdits());

        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag")));
        verify(editor, times(0)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("second-tag")));
        assertEquals(editResult.getErrorCode(), "spam-blacklisted-link");
        assertEquals(editResult.getErrorMessage(), "The text you wanted to publish was blocked by the spam filter.");
        assertEquals(editResult.getCorrespondingRowIds().size(), 1);
        assertTrue(editResult.getCorrespondingRowIds().contains(123));
    }

    @Test
    public void testGenericEditingError() throws Exception {
        List<EntityEdit> batch = new ArrayList<>();
        batch.add(new ItemEditBuilder(TestingData.existingId)
                .addAlias(Datamodel.makeMonolingualTextValue("see https://tinyurl.com/some_url", "en"))
                .addContributingRowId(123)
                .build());
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr")).build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag"))))
                .thenThrow(new MediaWikiApiErrorException("some-mediawiki-error", "Something wrong, not sure what"));

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag,
                Arrays.asList("first-tag", "second-tag"), 50, 60);
        assertEquals(1, processor.remainingEdits());
        EditResult editResult = processor.performEdit();
        assertEquals(0, processor.remainingEdits());

        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag")));
        verify(editor, times(0)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("second-tag")));
        assertEquals(editResult.getErrorCode(), "some-mediawiki-error");
        assertEquals(editResult.getErrorMessage(), "Something wrong, not sure what");
        assertEquals(editResult.getCorrespondingRowIds().size(), 1);
        assertTrue(editResult.getCorrespondingRowIds().contains(123));
    }

    @Test
    public void testIOException() throws Exception {
        List<EntityEdit> batch = new ArrayList<>();
        batch.add(new ItemEditBuilder(TestingData.existingId)
                .addAlias(Datamodel.makeMonolingualTextValue("see https://tinyurl.com/some_url", "en"))
                .addContributingRowId(123)
                .build());
        ItemDocument existingItem = ItemDocumentBuilder.forItemId(TestingData.existingId)
                .withLabel(Datamodel.makeMonolingualTextValue("pomme", "fr"))
                .withDescription(Datamodel.makeMonolingualTextValue("fruit délicieux", "fr")).build();
        when(fetcher.getEntityDocuments(Collections.singletonList(TestingData.existingId.getId())))
                .thenReturn(Collections.singletonMap(TestingData.existingId.getId(), existingItem));

        when(editor.editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag"))))
                .thenThrow(new IOException("connection to Wikibase failed"));

        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag,
                Arrays.asList("first-tag", "second-tag"), 50, 60);
        assertEquals(1, processor.remainingEdits());
        EditResult editResult = processor.performEdit();
        assertEquals(0, processor.remainingEdits());

        verify(editor, times(1)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("first-tag")));
        verify(editor, times(0)).editEntityDocument(any(), anyBoolean(), any(), eq(Collections.singletonList("second-tag")));
        assertEquals(editResult.getErrorCode(), "network-error");
        assertEquals(editResult.getErrorMessage(), "connection to Wikibase failed");
        assertEquals(editResult.getCorrespondingRowIds().size(), 1);
        assertTrue(editResult.getCorrespondingRowIds().contains(123));
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
        List<EntityEdit> batch = qids.stream()
                .map(qid -> new ItemEditBuilder(qid).addDescription(description, true).addContributingRowId(123).build())
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
        long revId = 1000L;
        for (ItemDocument doc : fullBatch) {
            when(editor.editEntityDocument(Datamodel.makeItemUpdate(doc.getEntityId(),
                    doc.getRevisionId(), Datamodel.makeTermUpdate(Collections.emptyList(), Collections.emptyList()),
                    Datamodel.makeTermUpdate(Collections.singletonList(description), Collections.emptyList()),
                    Collections.emptyMap(),
                    Datamodel.makeStatementUpdate(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                    Collections.emptyList(), Collections.emptyList()), false, summary, tags))
                            .thenReturn(new EditingResult(revId));
            revId++;
        }

        // Run edits
        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag, tags, batchSize,
                60);
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
        List<EntityEdit> batch = mids.stream()
                .map(mid -> new MediaInfoEditBuilder(mid).addLabel(label, false).addContributingRowId(123).build())
                .collect(Collectors.toList());

        int batchSize = 50;
        List<MediaInfoDocument> fullBatch = mids.stream()
                .map(mid -> Datamodel.makeMediaInfoDocument(mid)).collect(Collectors.toList());
        List<MediaInfoDocument> firstBatch = fullBatch.subList(0, batchSize);
        List<MediaInfoDocument> secondBatch = fullBatch.subList(batchSize, fullBatch.size());

        when(fetcher.getEntityDocuments(toMids(firstBatch))).thenReturn(toMapMediaInfo(firstBatch));
        when(fetcher.getEntityDocuments(toMids(secondBatch))).thenReturn(toMapMediaInfo(secondBatch));
        long revId = 1000L;
        for (MediaInfoDocument doc : fullBatch) {
            StatementUpdate statementUpdate = Datamodel.makeStatementUpdate(Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());
            when(editor.editEntityDocument(Datamodel.makeMediaInfoUpdate((MediaInfoIdValue) doc.getEntityId(),
                    doc.getRevisionId(), labelsUpdate, statementUpdate), false, summary, tags))
                            .thenReturn(new EditingResult(revId));
            revId++;
        }

        // Run edits
        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag, tags, batchSize,
                60);
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

    @Test
    public void testEditWikitext() throws MediaWikiApiErrorException, IOException, InterruptedException {
        MediaInfoIdValue mid = Datamodel.makeWikimediaCommonsMediaInfoIdValue("M12345");
        MediaInfoEdit edit = new MediaInfoEditBuilder(mid).addWikitext("my new wikitext").setOverrideWikitext(true)
                .addContributingRowId(123).build();
        List<EntityEdit> batch = Collections.singletonList(edit);
        List<MediaInfoDocument> existingDocuments = Collections.singletonList(Datamodel.makeMediaInfoDocument(mid));

        // mock CSRF token fetching
        String csrfToken = "9dd28471819";
        Map<String, String> params = new HashMap<>();
        params.put("action", "query");
        params.put("meta", "tokens");
        params.put("type", "csrf");
        when(connection.sendJsonRequest("POST", params))
                .thenReturn(ParsingUtilities.mapper.readTree("{\"batchcomplete\":\"\",\"query\":{\"tokens\":{"
                        + "\"csrftoken\":\"9dd28471819\"}}}"));

        // mock mediainfo document fetching
        when(fetcher.getEntityDocuments(toMids(existingDocuments))).thenReturn(toMapMediaInfo(existingDocuments));

        // mock page editing
        Map<String, String> editParams = new HashMap<>();
        editParams.put("action", "edit");
        editParams.put("tags", "my-tag");
        editParams.put("summary", summary);
        editParams.put("pageid", "12345");
        editParams.put("text", "my new wikitext");
        editParams.put("token", csrfToken);
        editParams.put("bot", "true");
        when(connection.sendJsonRequest("POST", editParams))
                .thenReturn(ParsingUtilities.mapper.readTree(successfulEditResponse));

        // Run the processor
        EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, batch, library, summary, maxlag, tags, 50,
                60);
        assertEquals(0, processor.progress());
        processor.performEdit();

        // sadly we cannot directly verify a method on the editor here since the editing of pages is not supported
        // there, but rather in our own MediaInfoUtils, so we resort to checking that the corresponding API call was
        // made at the connection level
        verify(connection, times(1)).sendJsonRequest("POST", editParams);
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
