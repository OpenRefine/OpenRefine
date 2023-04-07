
package org.openrefine.wikibase.editing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MaxlagErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.TokenErrorException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;

/**
 * Collection of wrappers around MediaWiki actions which are not supported by WDTK.
 * 
 * @author Antonin Delpeuch
 *
 */
public class MediaFileUtils {

    protected ApiConnection apiConnection;
    protected String csrfToken = null;
    protected int maxLagWaitTime = 5000; // ms
    protected String filePrefix = "File:"; // configurable?

    public MediaFileUtils(ApiConnection wdtkConnection) {
        apiConnection = wdtkConnection;
    }

    public ApiConnection getApiConnection() {
        return apiConnection;
    }

    /**
     * Sets how long we should wait before retrying in case of a maxlag error.
     * 
     * @param milliseconds
     */
    public void setMaxLagWaitTime(int milliseconds) {
        maxLagWaitTime = milliseconds;
    }

    /**
     * Purge the cache associated with a given MediaWiki page.
     * 
     * @throws MediaWikiApiErrorException
     * @throws IOException
     */
    public void purgePage(long pageid) throws IOException, MediaWikiApiErrorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "purge");
        parameters.put("pageids", Long.toString(pageid));

        apiConnection.sendJsonRequest("POST", parameters);
    }

    /**
     * Upload a local file to the MediaWiki instance.
     * 
     * @param path
     *            the path to the local file
     * @param fileName
     *            its filename once stored on the wiki
     * @param wikitext
     *            the accompanying wikitext for the file
     * @param summary
     *            the edit summary associated with the upload
     * @param tags
     *            tags to apply to the edit
     * @return
     * @throws IOException
     * @throws MediaWikiApiErrorException
     */
    public MediaUploadResponse uploadLocalFile(File path, String fileName, String wikitext, String summary, List<String> tags)
            throws IOException, MediaWikiApiErrorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "upload");
        parameters.put("tags", String.join("|", tags));
        parameters.put("comment", summary);
        parameters.put("filename", fileName);
        parameters.put("text", wikitext);
        parameters.put("token", getCsrfToken());
        Map<String, ImmutablePair<String, java.io.File>> files = new HashMap<>();
        files.put("file", new ImmutablePair<String, File>(fileName, path));

        return uploadFile(parameters, files);
    }

    /**
     * Upload a file that the MediaWiki server fetches directly from the supplied URL. The URL domain must likely be
     * whitelisted before.
     * 
     * @param url
     *            the URL of the file to upload
     * @param fileName
     *            its filename once stored on the wiki
     * @param wikitext
     *            the accompanying wikitext for the file
     * @param summary
     *            the edit summary associated with the upload
     * @param tags
     *            tags to apply to the edit
     * @return
     * @throws IOException
     * @throws MediaWikiApiErrorException
     */
    public MediaUploadResponse uploadRemoteFile(URL url, String fileName, String wikitext, String summary, List<String> tags)
            throws IOException, MediaWikiApiErrorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "upload");
        parameters.put("tags", String.join("|", tags));
        parameters.put("comment", summary);
        parameters.put("filename", fileName);
        parameters.put("text", wikitext);
        parameters.put("token", getCsrfToken());
        parameters.put("url", url.toExternalForm());
        Map<String, ImmutablePair<String, java.io.File>> files = Collections.emptyMap();

        return uploadFile(parameters, files);
    }

    /**
     * Edits the text contents of a wiki page
     * 
     * @param pageId
     *            the pageId of the page to edit
     * @param wikitext
     *            the new contents which should override the existing one
     * @param summary
     *            the edit summary
     * @param tags
     *            any tags that should be applied to the edit
     * @throws IOException
     *             if a network error happened
     * @throws MediaWikiApiErrorException
     *             if the editing failed for some reason, after a few retries
     */
    public void editPage(long pageId, String wikitext, String summary, List<String> tags) throws IOException, MediaWikiApiErrorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("action", "edit");
        parameters.put("tags", String.join("|", tags));
        parameters.put("summary", summary);
        parameters.put("pageid", Long.toString(pageId));
        parameters.put("text", wikitext);
        parameters.put("bot", "true");
        parameters.put("token", getCsrfToken());

        int retries = 3;
        MediaWikiApiErrorException lastException = null;
        while (retries > 0) {
            try {
                apiConnection.sendJsonRequest("POST", parameters);
                return;
            } catch (MediaWikiApiErrorException e) {
                lastException = e;
            }
            retries--;
        }
        throw lastException;
    }

    /**
     * Checks which of the provided page names already exist on the wiki.
     *
     * @param pageNames
     *            the page names (including namespace prefix)
     * @return
     * @throws IOException
     * @throws MediaWikiApiErrorException
     */
    public Set<String> checkIfPageNamesExist(List<String> pageNames) throws IOException, MediaWikiApiErrorException {
        // the site IRI provided to the fetcher is not important because it will not be exposed to the user
        WikibaseDataFetcher fetcher = new WikibaseDataFetcher(apiConnection, "http://some.site/iri");
        return fetcher.getMediaInfoIdsByFileName(pageNames).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Internal method, common to both local and remote file upload.
     * 
     * @param parameters
     * @param files
     * @return
     * @throws IOException
     * @throws MediaWikiApiErrorException
     */
    protected MediaUploadResponse uploadFile(Map<String, String> parameters, Map<String, ImmutablePair<String, java.io.File>> files)
            throws IOException, MediaWikiApiErrorException {
        int retries = 3;
        MediaWikiApiErrorException lastException = null;
        while (retries > 0) {
            try {
                JsonNode json = apiConnection.sendJsonRequest("POST", parameters, files);
                JsonNode uploadNode = json.get("upload");
                if (uploadNode == null) {
                    throw new IOException("The server did not return an 'upload' field in the JSON response.");
                }
                MediaUploadResponse response = ParsingUtilities.mapper.treeToValue(uploadNode, MediaUploadResponse.class);
                // todo check for errors which should be retried
                return response;
            } catch (TokenErrorException e) {
                lastException = e;
                // if the token was invalid, try again with a fresh one
                csrfToken = null;
                parameters.put("token", getCsrfToken());
            } catch (MaxlagErrorException e) { // wait for 5 seconds
                lastException = e;
                try {
                    Thread.sleep(maxLagWaitTime);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    retries = 0;
                }
            }
            retries--;
        }
        throw lastException;
    }

    protected String getCsrfToken() throws IOException, MediaWikiApiErrorException {
        if (csrfToken != null) {
            return csrfToken;
        }
        return fetchCsrfToken();
    }

    protected String fetchCsrfToken() throws IOException, MediaWikiApiErrorException {
        Map<String, String> params = new HashMap<>();
        params.put("action", "query");
        params.put("meta", "tokens");
        params.put("type", "csrf");

        JsonNode root = apiConnection.sendJsonRequest("POST", params);
        csrfToken = root.path("query").path("tokens").path("csrftoken").textValue();
        return csrfToken;
    }

    public static class MediaUploadResponse {

        @JsonProperty("result")
        public String result;
        @JsonProperty("filename")
        public String filename;
        @JsonProperty("pageid")
        public long pageid;
        @JsonProperty("warnings")
        public Map<String, JsonNode> warnings;

        @JsonIgnore
        private MediaInfoIdValue mid = null;

        /**
         * Checks that the upload was successful, and if not raise an exception
         * 
         * @throws MediaWikiApiErrorException
         */
        public void checkForErrors() throws MediaWikiApiErrorException {
            if (!"Success".equals(result)) {
                throw new MediaWikiApiErrorException(result,
                        "The file upload action returned the '" + result + "' error code. Warnings are: " + Objects.toString(warnings));
            }
            if (filename == null) {
                throw new MediaWikiApiErrorException(result, "The MediaWiki API did not return any filename for the uploaded file");
            }
        }

        /**
         * Retrieves the Mid, either from the upload response or by issuing another call to obtain it from the filename
         * through the supplied connection.
         * 
         * This should not be needed anymore when this is already exposed in the API response of the upload action.
         * https://phabricator.wikimedia.org/T307096
         * 
         * @param connection
         * @return
         * @throws MediaWikiApiErrorException
         * @throws IOException
         */
        public MediaInfoIdValue getMid(ApiConnection connection, String siteIri) throws IOException, MediaWikiApiErrorException {
            if (mid == null) {
                if (pageid == 0) {
                    WikibaseDataFetcher fetcher = new WikibaseDataFetcher(connection, siteIri);
                    mid = fetcher.getMediaInfoIdByFileName(filename);
                } else {
                    mid = Datamodel.makeMediaInfoIdValue(String.format("M%d", pageid), siteIri);
                }
            }
            return mid;
        }
    }
}
