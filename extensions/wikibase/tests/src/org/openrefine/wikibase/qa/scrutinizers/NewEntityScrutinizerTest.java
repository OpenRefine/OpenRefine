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

package org.openrefine.wikibase.qa.scrutinizers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.schema.entityvalues.SuggestedPropertyIdValue;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;

public class NewEntityScrutinizerTest extends ScrutinizerTest {

    private Claim claim = Datamodel.makeClaim(TestingData.newIdA,
            Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P31"), TestingData.existingId),
            Collections.emptyList());
    private Statement p31Statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    private Claim claimB = Datamodel.makeClaim(TestingData.newIdB,
            Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P31"), TestingData.existingId),
            Collections.emptyList());
    private Statement p31StatementB = Datamodel.makeStatement(claimB, Collections.emptyList(), StatementRank.NORMAL, "");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new NewEntityScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemEdit update = new ItemEditBuilder(TestingData.newIdA).addContributingRowId(123).build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.noDescType, NewEntityScrutinizer.noLabelType,
                NewEntityScrutinizer.noTypeType, NewEntityScrutinizer.newItemType);
    }

    @Test
    public void testEmptyItem() {
        ItemEdit update = new ItemEditBuilder(TestingData.existingId).addContributingRowId(123).build();
        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testGoodNewItem() {

        ItemEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("interesting item", "en"), true)
                .addStatement(add(p31Statement))
                .addContributingRowId(123)
                .build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newItemType);
    }

    @Test
    public void testDeletedStatements() {
        ItemEdit update = new ItemEditBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("interesting item", "en"), true)
                .addStatement(add(p31Statement))
                .addStatement(delete(TestingData.generateStatement(TestingData.newIdA, TestingData.matchedId)))
                .addContributingRowId(123)
                .build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newItemType, NewEntityScrutinizer.deletedStatementsType);
    }

    @Test
    public void testNewItemsWithDuplicateLabelAndDescription() {
        ItemEdit updateA = new ItemEditBuilder(TestingData.newIdA)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), false)
                .addDescription(Datamodel.makeMonolingualTextValue("description commune", "fr"), true)
                .addStatement(add(p31Statement))
                .addContributingRowId(123)
                .build();
        ItemEdit updateB = new ItemEditBuilder(TestingData.newIdB)
                .addLabel(Datamodel.makeMonolingualTextValue("bonjour", "fr"), true)
                .addDescription(Datamodel.makeMonolingualTextValue("description commune", "fr"), false)
                .addStatement(add(p31StatementB))
                .addContributingRowId(123)
                .build();

        scrutinize(updateA, updateB);

        assertWarningsRaised(NewEntityScrutinizer.newItemType, NewEntityScrutinizer.duplicateLabelDescriptionType);
    }

    @Test
    public void testNewMedia() {
        MediaInfoEdit update = new MediaInfoEditBuilder(TestingData.newMidA)
                .addContributingRowId(123)
                .build();
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newMediaType,
                NewEntityScrutinizer.newMediaWithoutFileNameType,
                NewEntityScrutinizer.newMediaWithoutFilePathType,
                NewEntityScrutinizer.newMediaWithoutWikitextType,
                NewEntityScrutinizer.newMediaMissingProperty);
    }

    @Test
    public void testInvalidFilePath() {
        MediaInfoEdit update = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFilePath("/this/path/does/not/exist.jpg")
                .addFileName("my_file.jpg")
                .addWikitext("description")
                .addContributingRowId(123)
                .build();
        scrutinizer.setEnableSlowChecks(true);
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newMediaType, NewEntityScrutinizer.invalidFilePathType,
                NewEntityScrutinizer.newMediaMissingProperty);
    }

    @Test
    public void testValidURL() {
        MediaInfoEdit update = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFilePath("https://foo.com/bar.jpg?type=blue")
                .addFileName("my_file.jpg")
                .addWikitext("description")
                .addContributingRowId(123)
                .build();
        scrutinizer.setEnableSlowChecks(true);
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newMediaType, NewEntityScrutinizer.newMediaMissingProperty);
    }

    @Test
    public void testInvalidFilePathFastMode() {
        MediaInfoEdit update = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFilePath("/this/path/does/not/exist.jpg")
                .addFileName("my_file.jpg")
                .addWikitext("description")
                .addContributingRowId(123)
                .build();
        scrutinizer.setEnableSlowChecks(false);
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newMediaType, NewEntityScrutinizer.newMediaMissingProperty);
    }

    @Test
    public void testFileShouldUploadInChunks() throws IOException {
        MediaInfoEdit update = mock(MediaInfoEdit.class);
        when(update.shouldUploadInChunks()).thenReturn(true);
        when(update.isNew()).thenReturn(true);
        Path file = Files.createTempFile("local-file", ".jpg");
        when(update.getFilePath()).thenReturn(file.toString());
        when(update.getFileName()).thenReturn("my_file.jpg");
        when(update.getWikitext()).thenReturn("description");
        scrutinizer.setEnableSlowChecks(true);
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newMediaType, NewEntityScrutinizer.newMediaChunkedUpload,
                NewEntityScrutinizer.newMediaMissingProperty);
    }

    @Test
    public void testNewMediaMandatoryPropertiesWarning() {
        MediaInfoEdit update = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFilePath("/this/path/does/not/exist.jpg")
                .addFileName("my_file.jpg")
                .addWikitext("=={{int:filedesc}}==\n" +
                        "{{Information\n" +
                        "|description={{en|1=Tarrafal Beach}}{{Wiki Loves Africa 2023 country|CV}}\n" +
                        "|date=2020-04-06 14:31:26\n" +
                        "|source={{own}}\n" +
                        "|author=[[User:Zico costa|Zico costa]]\n" +
                        "|permission=\n" +
                        "|other versions=\n" +
                        "}}")
                .addContributingRowId(123)
                .build();
        scrutinizer.setEnableSlowChecks(true);
        scrutinize(update);
        assertWarningsRaised(NewEntityScrutinizer.newMediaType, NewEntityScrutinizer.invalidFilePathType,
                NewEntityScrutinizer.newMediaMissingProperty);

        QAWarning warnP170 = new QAWarning("new-media-missing-property", "P170", QAWarning.Severity.CRITICAL, 1);
        warnP170.setFacetable(true);
        warnP170.setProperty("property_entity", new SuggestedPropertyIdValue("P170", "http://www.wikidata.org/entity/", ""));

        QAWarning warnP6216 = new QAWarning("new-media-missing-property", "P6216", QAWarning.Severity.CRITICAL, 1);
        warnP6216.setFacetable(true);
        warnP6216.setProperty("property_entity", new SuggestedPropertyIdValue("P6216", "http://www.wikidata.org/entity/", ""));

        QAWarning warnP7482 = new QAWarning("new-media-missing-property", "P7482", QAWarning.Severity.CRITICAL, 1);
        warnP7482.setFacetable(true);
        warnP7482.setProperty("property_entity", new SuggestedPropertyIdValue("P7482", "http://www.wikidata.org/entity/", ""));

        assertWarningRaised(warnP170);
        assertWarningRaised(warnP6216);
        assertWarningRaised(warnP7482);

    }
}
