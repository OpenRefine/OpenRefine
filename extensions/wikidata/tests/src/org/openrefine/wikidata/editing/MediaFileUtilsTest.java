
package org.openrefine.wikidata.editing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openrefine.wikidata.editing.MediaFileUtils.MediaUploadResponse;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.apierrors.MaxlagErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.TokenErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;

public class MediaFileUtilsTest {

    private static final String successfullUploadResponse = "{\"upload\":{\"result\":\"Success\",\"filename\":\"My_test_file.png\",\"pageid\":12345}}";
    private static final String csrfResponse = "{\"batchcomplete\":\"\",\"query\":{\"tokens\":{"
            + "\"csrftoken\":\"6f0da9b0e2626f86c5d862244d5faddd626a6eb2+\\\\\"}}}";
    private static final String csrfToken = "6f0da9b0e2626f86c5d862244d5faddd626a6eb2+\\";

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
        uploadParams.put("ignorewarnings", "0");
        JsonNode uploadJsonResponse = ParsingUtilities.mapper.readTree(
                successfullUploadResponse);
        when(connection.sendJsonRequest(eq("POST"), eq(uploadParams), any())).thenReturn(uploadJsonResponse);

        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
        File path = new File("/tmp/image.png");
        MediaUploadResponse response = mediaFileUtils.uploadLocalFile(path, "My_test_file.png", "my wikitext", "my summary",
                Collections.emptyList());
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
        uploadParams.put("ignorewarnings", "0");
        uploadParams.put("url", url);
        JsonNode uploadJsonResponse = ParsingUtilities.mapper.readTree(successfullUploadResponse);
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
        invalidParams.put("ignorewarnings", "0");
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
        uploadParams.put("ignorewarnings", "0");
        uploadParams.put("url", url);
        JsonNode uploadJsonResponse = ParsingUtilities.mapper.readTree(successfullUploadResponse);
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
        uploadParams.put("ignorewarnings", "0");
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

    protected void mockCsrfCall(ApiConnection connection) throws IOException, MediaWikiApiErrorException {
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("action", "query");
        tokenParams.put("meta", "tokens");
        tokenParams.put("type", "csrf");
        JsonNode tokenJsonResponse = ParsingUtilities.mapper.readTree(csrfResponse);
        when(connection.sendJsonRequest("POST", tokenParams)).thenReturn(tokenJsonResponse);
    }
};;
