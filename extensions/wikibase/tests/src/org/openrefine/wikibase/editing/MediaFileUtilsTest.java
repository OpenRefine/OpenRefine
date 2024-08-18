
package org.openrefine.wikibase.editing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.MaxlagErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.TokenErrorException;

import com.google.refine.util.ParsingUtilities;

import org.openrefine.wikibase.editing.MediaFileUtils.ChunkedFile;
import org.openrefine.wikibase.editing.MediaFileUtils.MediaUploadResponse;

public class MediaFileUtilsTest {

    private static final String successfulUploadResponse = "{\"upload\":{\"result\":\"Success\",\"filename\":\"My_test_file.png\",\"pageid\":12345}}";
    private static final String csrfResponse = "{\"batchcomplete\":\"\",\"query\":{\"tokens\":{"
            + "\"csrftoken\":\"6f0da9b0e2626f86c5d862244d5faddd626a6eb2+\\\\\"}}}";
    private static final String csrfToken = "6f0da9b0e2626f86c5d862244d5faddd626a6eb2+\\";
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

    private static final String successfulEditResponseNoFilename = "{\n"
            + "    \"edit\": {\n"
            + "        \"result\": \"Success\",\n"
            + "        \"contentmodel\": \"wikitext\",\n"
            + "        \"oldrevid\": 371705,\n"
            + "        \"newrevid\": 371707,\n"
            + "        \"newtimestamp\": \"2018-12-18T16:59:42Z\"\n"
            + "    }\n"
            + "}";
    private static final String unsuccessfulEditResponse = "{\n"
            + "    \"edit\": {\n"
            + "        \"result\": \"InvalidMediaFile\"\n"
            + "    }\n"
            + "}";

    @Test
    public void testPurge() throws IOException, MediaWikiApiErrorException {
        ApiConnection connection = mock(ApiConnection.class);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        mediaFileUtils.purgePage(12345L);

        Map<String, String> params = new HashMap<>();
        params.put("action", "purge");
        params.put("pageids", "12345");
        verify(connection, times(1)).sendJsonRequest("POST", params);
    }

    @Test
    public void testSuccessfulLocalUpload() throws IOException, MediaWikiApiErrorException {
        ApiConnection connection = mock(ApiConnection.class);

        // mock CSRF token request
        mockCsrfCall(connection);

        // mock file upload request
        Map<String, String> uploadParams = new HashMap<>();
        uploadParams.put("action", "upload");
        uploadParams.put("tags", "");
        uploadParams.put("comment", "my summary");
        uploadParams.put("filename", "My_test_file.png");
        uploadParams.put("text", "my wikitext");
        uploadParams.put("token", csrfToken);
        JsonNode uploadJsonResponse = ParsingUtilities.mapper.readTree(
                successfulUploadResponse);
        when(connection.sendJsonRequest(eq("POST"), eq(uploadParams), any())).thenReturn(uploadJsonResponse);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        File path = new File("/tmp/image.png");
        MediaUploadResponse response = mediaFileUtils.uploadLocalFile(path, "My_test_file.png", "my wikitext", "my summary",
                Collections.emptyList(), false);
        assertEquals(response.filename, "My_test_file.png");
        assertEquals(response.pageid, 12345L);
        assertEquals(response.getMid(connection, Datamodel.SITE_WIKIMEDIA_COMMONS),
                Datamodel.makeWikimediaCommonsMediaInfoIdValue("M12345"));
    }

    @Test
    public void testSuccessfulRemoteUpload() throws IOException, MediaWikiApiErrorException {
        String url = "https://foo.com/file.png";
        ApiConnection connection = mock(ApiConnection.class);

        // mock CSRF token request
        mockCsrfCall(connection);

        // mock file upload request
        Map<String, String> uploadParams = new HashMap<>();
        uploadParams.put("action", "upload");
        uploadParams.put("tags", "");
        uploadParams.put("comment", "my summary");
        uploadParams.put("filename", "My_test_file.png");
        uploadParams.put("text", "my wikitext");
        uploadParams.put("token", csrfToken);
        uploadParams.put("url", url);
        JsonNode uploadJsonResponse = ParsingUtilities.mapper.readTree(successfulUploadResponse);
        when(connection.sendJsonRequest("POST", uploadParams, Collections.emptyMap())).thenReturn(uploadJsonResponse);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        // For this test, assume the CSRF token has already been fetched
        mediaFileUtils.fetchCsrfToken();

        MediaUploadResponse response = mediaFileUtils.uploadRemoteFile(new URL(url), "My_test_file.png", "my wikitext", "my summary",
                Collections.emptyList());

        assertEquals(response.filename, "My_test_file.png");
        assertEquals(response.pageid, 12345L);
        assertEquals(response.getMid(connection, Datamodel.SITE_WIKIMEDIA_COMMONS),
                Datamodel.makeWikimediaCommonsMediaInfoIdValue("M12345"));
    }

    @Test
    public void testSuccessfulRemoteUploadInvalidToken() throws IOException, MediaWikiApiErrorException {
        String url = "https://foo.com/file.png";
        ApiConnection connection = mock(ApiConnection.class);

        // mock CSRF token request
        mockCsrfCall(connection);

        // mock request with invalid token
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put("action", "upload");
        invalidParams.put("tags", "");
        invalidParams.put("comment", "my summary");
        invalidParams.put("filename", "My_test_file.png");
        invalidParams.put("text", "my wikitext");
        invalidParams.put("token", "invalid_token");
        invalidParams.put("url", url);
        when(connection.sendJsonRequest("POST", invalidParams, Collections.emptyMap()))
                .thenThrow(new TokenErrorException("wrongtoken", "looks bad"));

        // mock file upload request
        Map<String, String> uploadParams = new HashMap<>();
        uploadParams.put("action", "upload");
        uploadParams.put("tags", "");
        uploadParams.put("comment", "my summary");
        uploadParams.put("filename", "My_test_file.png");
        uploadParams.put("text", "my wikitext");
        uploadParams.put("token", csrfToken);
        uploadParams.put("url", url);
        JsonNode uploadJsonResponse = ParsingUtilities.mapper.readTree(successfulUploadResponse);
        when(connection.sendJsonRequest("POST", uploadParams, Collections.emptyMap()))
                .thenReturn(uploadJsonResponse);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        mediaFileUtils.csrfToken = "invalid_token";

        MediaUploadResponse response = mediaFileUtils.uploadRemoteFile(new URL(url), "My_test_file.png", "my wikitext", "my summary",
                Collections.emptyList());

        // the request still succeeds because we retry with a fresh CSRF
        assertEquals(response.filename, "My_test_file.png");
        assertEquals(response.pageid, 12345L);
        assertEquals(response.getMid(connection, Datamodel.SITE_WIKIMEDIA_COMMONS),
                Datamodel.makeWikimediaCommonsMediaInfoIdValue("M12345"));
    }

    @Test(expectedExceptions = MaxlagErrorException.class)
    public void testMaxlagException() throws IOException, MediaWikiApiErrorException {
        String url = "https://foo.com/file.png";
        ApiConnection connection = mock(ApiConnection.class);

        // mock CSRF token request
        mockCsrfCall(connection);

        // mock file upload request
        Map<String, String> uploadParams = new HashMap<>();
        uploadParams.put("action", "upload");
        uploadParams.put("tags", "");
        uploadParams.put("comment", "my summary");
        uploadParams.put("filename", "My_test_file.png");
        uploadParams.put("text", "my wikitext");
        uploadParams.put("token", csrfToken);
        uploadParams.put("url", url);
        when(connection.sendJsonRequest("POST", uploadParams, Collections.emptyMap())).thenThrow(
                new MaxlagErrorException("the server is too slow"));

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        mediaFileUtils.setMaxLagWaitTime(10);

        mediaFileUtils.uploadRemoteFile(new URL(url), "My_test_file.png", "my wikitext", "my summary", Collections.emptyList());
    }

    @Test
    public void testGetMidFromFilename() throws IOException, MediaWikiApiErrorException {
        ApiConnection connection = mock(ApiConnection.class);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "query");
        parameters.put("titles", "File:Commons-logo.svg");

        JsonNode jsonResponse = ParsingUtilities.mapper.readTree("{"
                + "\"continue\":{\"iistart\":\"2014-04-10T10:05:06Z\",\"continue\":\"||\"},"
                + "\"query\":{\"pages\":{\"317966\":"
                + "{\"pageid\":317966,\"ns\":6,\"title\":\"File:Commons-logo.svg\","
                + "\"imagerepository\":\"local\",\"imageinfo\":[{\"timestamp\":\"2014-06-03T13:43:45Z\",\"user\":\"Steinsplitter\"}]}}}}");

        when(connection.sendJsonRequest("POST", parameters)).thenReturn(jsonResponse);

        MediaUploadResponse response = ParsingUtilities.mapper.readValue("{\"result\":\"Success\",\"filename\":\"Commons-logo.svg\"}",
                MediaUploadResponse.class);
        MediaInfoIdValue mid = response.getMid(connection, "http://commons.wikimedia.org/entity/");
        assertEquals(mid, Datamodel.makeWikimediaCommonsMediaInfoIdValue("M317966"));
    }

    @Test
    public void testEditPage() throws IOException, MediaWikiApiErrorException {
        ApiConnection connection = mock(ApiConnection.class);

        // mock CSRF token request
        mockCsrfCall(connection);

        // mock wikitext edit call
        Map<String, String> uploadParams = new HashMap<>();
        uploadParams.put("action", "edit");
        uploadParams.put("tags", "");
        uploadParams.put("summary", "my summary");
        uploadParams.put("pageid", "12345");
        uploadParams.put("text", "my new wikitext");
        uploadParams.put("token", csrfToken);
        uploadParams.put("bot", "true");
        JsonNode editJsonResponse = ParsingUtilities.mapper.readTree(successfulEditResponse);
        when(connection.sendJsonRequest("POST", uploadParams)).thenReturn(editJsonResponse);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        // For this test, assume the CSRF token has already been fetched
        mediaFileUtils.fetchCsrfToken();

        long revisionId = mediaFileUtils.editPage(12345L, "my new wikitext", "my summary", Collections.emptyList());

        assertEquals(revisionId, 371707L);
        // check the requests were done as expected
        InOrder inOrder = Mockito.inOrder(connection);
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("action", "query");
        tokenParams.put("meta", "tokens");
        tokenParams.put("type", "csrf");
        inOrder.verify(connection, times(1)).sendJsonRequest("POST", tokenParams);
        inOrder.verify(connection, times(1)).sendJsonRequest("POST", uploadParams);
    }

    @Test
    public void testCheckIfPageNamesExist() throws IOException, MediaWikiApiErrorException {
        ApiConnection connection = mock(ApiConnection.class);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("action", "query");
        queryParams.put("titles", "File:Does_not_exist|File:Does_exist");
        String pageQuery = "{" +
                "\"query\":{" +
                "    \"normalized\": [" +
                "           {\"from\":\"File:Does_not_exist\",\"to\":\"File:Does not exist\"}," +
                "           {\"from\":\"File:Does_exist\",\"to\":\"File:Does exist\"}" +
                "    ]," +
                "    \"pages\": {" +
                "       \"-1\": {" +
                "          \"ns\": 4," +
                "          \"title\": \"File:Does not exist\"," +
                "          \"missing\": \"\"" +
                "       }," +
                "       \"132\": {" +
                "           \"ns\": 4," +
                "           \"title\": \"File:Does exist\"," +
                "           \"pageid\": 132" +
                "       }" +
                "    }" +
                "  }" +
                "}";
        JsonNode jsonResponse = ParsingUtilities.mapper.readTree(pageQuery);

        when(connection.sendJsonRequest("POST", queryParams)).thenReturn(jsonResponse);

        MediaFileUtils SUT = new MediaFileUtils(connection);

        Set<String> existing = SUT.checkIfPageNamesExist(Arrays.asList("Does_not_exist", "Does_exist"));

        assertEquals(existing, Collections.singleton("Does_exist"));
    }

    @Test
    public void testUploadError() throws JsonProcessingException {
        MediaFileUtils.MediaUploadResponse response = ParsingUtilities.mapper.readValue(unsuccessfulEditResponse,
                MediaUploadResponse.class);

        assertThrows(MediaWikiApiErrorException.class, () -> response.checkForErrors());
    }

    @Test
    public void testUploadSuccessNoFilename() throws JsonProcessingException {
        MediaFileUtils.MediaUploadResponse response = ParsingUtilities.mapper.readValue(successfulEditResponseNoFilename,
                MediaUploadResponse.class);

        assertThrows(MediaWikiApiErrorException.class, () -> response.checkForErrors());
    }

    protected void mockCsrfCall(ApiConnection connection) throws IOException, MediaWikiApiErrorException {
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("action", "query");
        tokenParams.put("meta", "tokens");
        tokenParams.put("type", "csrf");
        JsonNode tokenJsonResponse = ParsingUtilities.mapper.readTree(csrfResponse);
        when(connection.sendJsonRequest("POST", tokenParams)).thenReturn(tokenJsonResponse);
    }

    @Test
    public void testUploadLocalFileChunked() throws IOException, MediaWikiApiErrorException {
        ApiConnection connection = mock(ApiConnection.class);
        // mock CSRF token request
        mockCsrfCall(connection);

        ChunkedFile chunkedFile = mock(ChunkedFile.class);
        when(chunkedFile.getLength()).thenReturn(10001L);
        when(chunkedFile.getExtension()).thenReturn(".png");
        Path firstChunk = Files.createTempFile("chunk-1-", ".png");
        Path secondChunk = Files.createTempFile("chunk-2-", ".png");
        Path thirdChunk = Files.createTempFile("chunk-3-", ".png");
        when(chunkedFile.readChunk())
                .thenReturn(firstChunk.toFile())
                .thenReturn(secondChunk.toFile())
                .thenReturn(thirdChunk.toFile())
                .thenReturn(null);

        // Initialise the upload and upload the first chunk.
        Map<String, String> firstParams = new HashMap<>();
        firstParams.put("action", "upload");
        firstParams.put("filename", "My_test_file.png");
        firstParams.put("stash", "1");
        firstParams.put("filesize", "10001");
        firstParams.put("offset", "0");
        firstParams.put("token", csrfToken);
        String firstResponseString = "{\"upload\":{\"offset\":5000,\"result\":\"Continue\",\"filekey\":\"filekey.1234.png\"}}";
        JsonNode firstResponse = ParsingUtilities.mapper.readTree(firstResponseString);
        Map<String, ImmutablePair<String, java.io.File>> firstFiles = new HashMap<>();
        firstFiles.put("chunk", new ImmutablePair<String, File>("chunk-1.png", firstChunk.toFile()));
        when(connection.sendJsonRequest(eq("POST"), eq(firstParams), eq(firstFiles))).thenReturn(firstResponse);

        // Upload the second chunk.
        Map<String, String> secondParams = new HashMap<>();
        secondParams.put("action", "upload");
        secondParams.put("filename", "My_test_file.png");
        secondParams.put("stash", "1");
        secondParams.put("filesize", "10001");
        secondParams.put("offset", "5000");
        secondParams.put("filekey", "filekey.1234.png");
        secondParams.put("token", csrfToken);
        String secondResponseString = "{\"upload\":{\"offset\":10000,\"result\":\"Continue\",\"filekey\":\"filekey.1234.png\"}}";
        JsonNode secondResponse = ParsingUtilities.mapper.readTree(secondResponseString);
        Map<String, ImmutablePair<String, java.io.File>> secondFiles = new HashMap<>();
        secondFiles.put("chunk", new ImmutablePair<String, File>("chunk-2.png", secondChunk.toFile()));
        when(connection.sendJsonRequest(eq("POST"), eq(secondParams), eq(secondFiles))).thenReturn(secondResponse);

        // Upload the third and final chunk.
        Map<String, String> thirdParams = new HashMap<>();
        thirdParams.put("action", "upload");
        thirdParams.put("filename", "My_test_file.png");
        thirdParams.put("stash", "1");
        thirdParams.put("filesize", "10001");
        thirdParams.put("offset", "10000");
        thirdParams.put("filekey", "filekey.1234.png");
        thirdParams.put("token", csrfToken);
        String thirdResponseString = "{\"upload\":{\"offset\":10001,\"result\":\"Continue\",\"filekey\":\"filekey.1234.png\"}}";
        JsonNode thirdResponse = ParsingUtilities.mapper.readTree(
                thirdResponseString);
        Map<String, ImmutablePair<String, java.io.File>> thirdFiles = new HashMap<>();
        thirdFiles.put("chunk", new ImmutablePair<String, File>("chunk-3.png", thirdChunk.toFile()));
        when(connection.sendJsonRequest(eq("POST"), eq(thirdParams), eq(thirdFiles))).thenReturn(thirdResponse);

        // Finalise the upload.
        Map<String, String> finalParams = new HashMap<>();
        finalParams.put("action", "upload");
        finalParams.put("filename", "My_test_file.png");
        finalParams.put("filekey", "filekey.1234.png");
        finalParams.put("tags", "");
        finalParams.put("comment", "my summary");
        finalParams.put("text", "my wikitext");
        finalParams.put("token", csrfToken);
        JsonNode finalResponse = ParsingUtilities.mapper.readTree(successfulUploadResponse);
        when(connection.sendJsonRequest(eq("POST"), eq(finalParams), eq(null))).thenReturn(finalResponse);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        MediaUploadResponse response = mediaFileUtils.uploadLocalFileChunked(chunkedFile, "My_test_file.png", "my wikitext", "my summary",
                Collections.emptyList());

        InOrder inOrder = inOrder(connection);
        inOrder.verify(connection).sendJsonRequest(eq("POST"), eq(firstParams), eq(firstFiles));
        inOrder.verify(connection).sendJsonRequest(eq("POST"), eq(secondParams), eq(secondFiles));
        inOrder.verify(connection).sendJsonRequest(eq("POST"), eq(thirdParams), eq(thirdFiles));
        inOrder.verify(connection).sendJsonRequest(eq("POST"), eq(finalParams), eq(null));
        assertEquals(response.filename, "My_test_file.png");
        assertEquals(response.pageid, 12345L);
        assertEquals(response.getMid(connection, Datamodel.SITE_WIKIMEDIA_COMMONS),
                Datamodel.makeWikimediaCommonsMediaInfoIdValue("M12345"));
    }
};;
