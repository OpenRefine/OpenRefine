
package org.openrefine.wikibase.updates;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikibase.editing.MediaFileUtils;
import org.openrefine.wikibase.editing.MediaFileUtils.MediaUploadResponse;
import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import com.google.refine.util.TestUtils;

public class MediaInfoEditTest {

    private MediaInfoIdValue existingSubject = Datamodel.makeWikimediaCommonsMediaInfoIdValue("M5678");
    private MediaInfoIdValue newSubject = TestingData.makeNewMediaInfoIdValue(1234L, "new item");

    private PropertyIdValue pid1 = Datamodel.makeWikidataPropertyIdValue("P348");
    private PropertyIdValue pid2 = Datamodel.makeWikidataPropertyIdValue("P52");
    private Claim claim1 = Datamodel.makeClaim(existingSubject, Datamodel.makeNoValueSnak(pid1),
            Collections.emptyList());
    private Claim claim1New = Datamodel.makeClaim(newSubject, Datamodel.makeNoValueSnak(pid1),
            Collections.emptyList());
    private Claim claim2 = Datamodel.makeClaim(existingSubject, Datamodel.makeValueSnak(pid2, Datamodel.makeStringValue("foo")),
            Collections.emptyList());
    private Statement statement1 = Datamodel.makeStatement(claim1, Collections.emptyList(), StatementRank.NORMAL, "");
    private Statement statement1New = Datamodel.makeStatement(claim1New, Collections.emptyList(), StatementRank.NORMAL, "");
    private Statement statement2 = Datamodel.makeStatement(claim2, Collections.emptyList(), StatementRank.NORMAL, "");
    private StatementMerger strategy = new PropertyOnlyStatementMerger();
    private StatementEdit statementUpdate1 = new StatementEdit(statement1, strategy, StatementEditingMode.ADD_OR_MERGE);
    private StatementEdit statementUpdate1New = new StatementEdit(statement1New, strategy, StatementEditingMode.ADD_OR_MERGE);
    private StatementEdit statementUpdate2 = new StatementEdit(statement2, strategy, StatementEditingMode.DELETE);
    private MonolingualTextValue label = Datamodel.makeMonolingualTextValue("this is a label", "en");

    private Set<StatementGroupEdit> statementGroups;

    public MediaInfoEditTest() {
        statementGroups = new HashSet<>();
        statementGroups.add(new StatementGroupEdit(Collections.singletonList(statementUpdate1)));
        statementGroups.add(new StatementGroupEdit(Collections.singletonList(statementUpdate2)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateWithoutSubject() {
        new MediaInfoEditBuilder(null);
    }

    @Test
    public void testAddStatements() {
        MediaInfoEdit update = new MediaInfoEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        assertFalse(update.isNull());
        assertEquals(Arrays.asList(statementUpdate1, statementUpdate2), update.getStatementEdits());
        assertEquals(statementGroups, update.getStatementGroupEdits().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testSerializeStatements() throws IOException {
        MediaInfoEdit update = new MediaInfoEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        TestUtils.isSerializedTo(update, TestingData.jsonFromFile("updates/mediainfo_update.json"));
    }

    @Test
    public void testMerge() {
        MediaInfoEdit updateA = new MediaInfoEditBuilder(existingSubject).addStatement(statementUpdate1).build();
        MediaInfoEdit updateB = new MediaInfoEditBuilder(existingSubject).addStatement(statementUpdate2).build();
        assertNotEquals(updateA, updateB);
        MediaInfoEdit merged = updateA.merge(updateB);
        assertEquals(statementGroups, merged.getStatementGroupEdits().stream().collect(Collectors.toSet()));
    }

    @Test
    public void testToEntityUpdate() {
        MediaInfoEdit edit = new MediaInfoEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .addFileName("Foo.png")
                .addFilePath("C:\\Foo.png")
                .build();
        assertTrue(edit.requiresFetchingExistingState());

        FullMediaInfoUpdate update = edit.toEntityUpdate(Datamodel.makeMediaInfoDocument(existingSubject));
        assertEquals(update.getStatements().getAdded(), Arrays.asList(statement1));
        assertEquals(update.getFileName(), "Foo.png");
        assertEquals(update.getFilePath(), "C:\\Foo.png");
    }

    @Test
    public void testToEntityUpdateOverridingWikitext() {
        MediaInfoEdit edit = new MediaInfoEditBuilder(existingSubject)
                .addWikitext("my new wikitext")
                .setOverrideWikitext(true)
                .build();
        assertFalse(edit.requiresFetchingExistingState());

        FullMediaInfoUpdate update = edit.toEntityUpdate(null);
        assertEquals(update.getStatements().getAdded(), Collections.emptyList());
        assertEquals(update.getFileName(), null);
        assertEquals(update.getFilePath(), null);
        assertEquals(update.getWikitext(), "my new wikitext");
        assertEquals(update.isOverridingWikitext(), true);
    }

    @Test
    public void testUploadNewFile() throws MediaWikiApiErrorException, IOException {
        String url = "https://my.site.com/file.png";
        MediaInfoEdit edit = new MediaInfoEditBuilder(newSubject)
                .addStatement(statementUpdate1New)
                .addFileName("Foo.png")
                .addFilePath(url)
                .addWikitext("{{wikitext}}")
                .build();
        assertFalse(edit.requiresFetchingExistingState()); // new entities do not require fetching existing state

        // set up dependencies
        WikibaseDataEditor editor = mock(WikibaseDataEditor.class);
        MediaFileUtils mediaFileUtils = mock(MediaFileUtils.class);
        MediaUploadResponse response = mock(MediaUploadResponse.class);
        MediaInfoIdValue mid = Datamodel.makeMediaInfoIdValue("M1234", "http://www.wikidata.org/entity/");
        when(response.getMid(any(), any()))
                .thenReturn(mid);
        when(mediaFileUtils.uploadRemoteFile(new URL(url), "Foo.png", "{{wikitext}}\n[[Category:Uploaded with OpenRefine]]", "summary",
                Collections.emptyList()))
                        .thenReturn(response);

        MediaInfoIdValue returnedMid = edit.uploadNewFile(editor, mediaFileUtils, "summary", Collections.emptyList());
        assertEquals(returnedMid, mid);
    }

    @Test
    public void testToString() {
        MediaInfoEdit edit = new MediaInfoEditBuilder(existingSubject).addStatement(statementUpdate1)
                .addStatement(statementUpdate2)
                .build();
        assertTrue(edit.toString().contains("M5678"));
    }

}
