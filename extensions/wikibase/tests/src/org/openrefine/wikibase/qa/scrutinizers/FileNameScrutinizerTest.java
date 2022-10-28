
package org.openrefine.wikibase.qa.scrutinizers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FileNameScrutinizerTest extends ScrutinizerTest {

    private static JsonNode apiResponseFound;

    static {
        try {
            apiResponseFound = ParsingUtilities.mapper.readTree(
                    "{\"query\":{\"pages\":{\"123\":{\"ns\":1,\"title\":\"File:Does exist.png\",\"pageid\":123}}}}}");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public EditScrutinizer getScrutinizer() {
        return new FileNameScrutinizer();
    }

    @Test
    public void testSameFileNameTwice() throws IOException, MediaWikiApiErrorException {
        MediaInfoEdit edit1 = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("Some_filename.png")
                .build();

        MediaInfoEdit edit2 = new MediaInfoEditBuilder(TestingData.newMidB)
                .addFileName("some filename.png")
                .build();

        scrutinize(edit1, edit2);

        // the two file names are different, but equal after normalization,
        // so we should report a conflict.
        assertWarningsRaised(FileNameScrutinizer.duplicateFileNamesInBatchType);
    }

    @Test
    public void testLongFileName() throws IOException, MediaWikiApiErrorException {
        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("Some_very_very_very_very_very_very_very_very" +
                        "very_very_very_very_very_very_very_very_very_very_" +
                        "very_very_very_very_very_very_very_very_very_very_" +
                        "very_very_very_very_very_very_very_very_very_very_" +
                        "very_very_very_very_very_very_very_very_very_very_" +
                        "long_filename.png")
                .build();

        scrutinize(edit);
        assertWarningsRaised(FileNameScrutinizer.fileNameTooLongType);
    }

    @Test
    public void testInvalidCharactersInFilenameVerticalBar() throws IOException, MediaWikiApiErrorException {
        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("vertical bars (|) are not allowed.png")
                .build();

        scrutinize(edit);
        assertWarningsRaised(FileNameScrutinizer.invalidCharactersInFileNameType);
    }

    @Test
    public void testInvalidCharactersInFilenameHTMLEscaped() throws IOException, MediaWikiApiErrorException {
        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("HTML escaped entities such as &nbsp; are not allowed.png")
                .build();

        scrutinize(edit);
        assertWarningsRaised(FileNameScrutinizer.invalidCharactersInFileNameType);
    }

    @Test
    public void testNoExtension() throws IOException, MediaWikiApiErrorException {
        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("Look, no extension")
                .build();

        scrutinize(edit);
        assertWarningsRaised(FileNameScrutinizer.missingFileNameExtensionType);
    }

    @Test
    public void testInconsistentExtensions() throws IOException, MediaWikiApiErrorException {
        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("Some_image.png")
                .addFilePath("tmp/Some_sound.ogg")
                .build();

        scrutinize(edit);
        assertWarningsRaised(FileNameScrutinizer.inconsistentFileNameAndPathExtensionType);
    }

    @Test
    public void testAlreadyExistsOnWiki() throws IOException, MediaWikiApiErrorException {
        // mock API call to search for existing filenames
        when(connection.sendJsonRequest(any(), any())).thenReturn(apiResponseFound);

        scrutinizer.setEnableSlowChecks(true);

        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("Does exist.png")
                .build();

        scrutinize(edit);

        assertWarningsRaised(FileNameScrutinizer.fileNamesAlreadyExistOnWikiType);
    }

    @Test
    public void testAlreadyExistsOnWikiSkippedInFastMode() throws IOException, MediaWikiApiErrorException {
        // in fast mode, we do not check if files already exist on the wiki
        scrutinizer.setEnableSlowChecks(false);

        MediaInfoEdit edit = new MediaInfoEditBuilder(TestingData.newMidA)
                .addFileName("Does exist.png")
                .build();

        scrutinize(edit);

        assertNoWarningRaised();
        verifyNoInteractions(connection);
    }
}
