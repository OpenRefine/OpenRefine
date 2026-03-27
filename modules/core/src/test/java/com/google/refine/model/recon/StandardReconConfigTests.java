/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.model.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.SocketEffect;
import okhttp3.HttpUrl;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.model.recon.StandardReconConfig.ColumnDetail;
import com.google.refine.model.recon.StandardReconConfig.ReconResult;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class StandardReconConfigTests extends RefineTest {

    String QUERY_TEXT = "david lynch";
    String QUERY_JSON = "{\"query\":\"david lynch\",\"type\":\"Q11424\",\"properties\":[{\"pid\":\"P57\",\"v\":\"david lynch\"}],\"type_strict\":\"should\"}";

    @BeforeMethod
    public void registerOperation() {
        ReconConfig.registerReconConfig(getCoreModule(), "standard-service", StandardReconConfig.class);
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private static class StandardReconConfigStub extends StandardReconConfig {

        public StandardReconConfigStub() {
            super("", "", "", "", "", false, new ArrayList<ColumnDetail>());
        }

        public double wordDistanceTest(String s1, String s2) {
            return wordDistance(s1, s2);
        }

        protected Recon createReconServiceResults(String text, ArrayNode resultsList, long historyEntryID) {
            return super.createReconServiceResults(text, resultsList, historyEntryID);
        }
    }

    @Test
    public void wordDistance() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("Foo", "Foo bar");

        Assert.assertEquals(r, 0.5);
    }

    @Test
    public void wordDistanceOnlyStopwords() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("On and On", "On and On and On");

        Assert.assertFalse(Double.isInfinite(r));
        Assert.assertFalse(Double.isNaN(r));
    }

    @Test
    public void serializeStandardReconConfig() throws Exception {
        String json = """
                 {
                        "mode": "standard-service",
                        "service": "https://tools.wmflabs.org/openrefine-wikidata/en/api",
                        "identifierSpace": "http://www.wikidata.org/entity/",
                        "schemaSpace": "http://www.wikidata.org/prop/direct/",
                        "type": {
                                "id": "Q13442814",
                                "name": "scientific article"
                        },
                        "autoMatch": true,
                        "columnDetails": [
                           {
                             "column": "organization_country",
                             "propertyName": "SPARQL: P17/P297",
                             "propertyID": "P17/P297"
                           },
                           {
                             "column": "organization_id",
                             "propertyName": "SPARQL: P3500|P2427",
                             "propertyID": "P3500|P2427"
                           }
                        ],
                        "limit": 0
                 }
                """;
        ReconConfig config = ReconConfig.reconstruct(json);
        TestUtils.isSerializedTo(config, json);

        // the "mode" only appears once in the serialization result
        String fullJson = ParsingUtilities.mapper.writeValueAsString(config);
        assertEquals(fullJson.indexOf("\"mode\"", fullJson.indexOf("\"mode\"") + 1), -1);
    }

    @Test
    public void serializeStandardReconConfigWithBatchSize() throws Exception {
        String json = """
                 {
                        "mode": "standard-service",
                        "service": "https://tools.wmflabs.org/openrefine-wikidata/en/api",
                        "identifierSpace": "http://www.wikidata.org/entity/",
                        "schemaSpace": "http://www.wikidata.org/prop/direct/",
                        "type": {
                                "id": "Q13442814",
                                "name": "scientific article"
                        },
                        "autoMatch": true,
                        "batchSize": 20,
                        "columnDetails": [
                           {
                             "column": "organization_country",
                             "propertyName": "SPARQL: P17/P297",
                             "propertyID": "P17/P297"
                           },
                           {
                             "column": "organization_id",
                             "propertyName": "SPARQL: P3500|P2427",
                             "propertyID": "P3500|P2427"
                           }
                        ],
                        "limit": 0
                 }
                """;
        ReconConfig config = ReconConfig.reconstruct(json);
        TestUtils.isSerializedTo(config, json);

        // the "mode" only appears once in the serialization result
        String fullJson = ParsingUtilities.mapper.writeValueAsString(config);
        assertEquals(fullJson.indexOf("\"mode\"", fullJson.indexOf("\"mode\"") + 1), -1);
    }

    @Test
    public void testReconstructNoType() throws IOException {
        String json = """
                {"mode":"standard-service",
                "service":"https://tools.wmflabs.org/openrefine-wikidata/en/api",
                "identifierSpace":"http://www.wikidata.org/entity/",
                "schemaSpace":"http://www.wikidata.org/prop/direct/",
                "type":null,
                "autoMatch":true,
                "columnDetails":[
                    {"column":"_ - id",
                     "property":{"id":"P3153","name":"Crossref funder ID"}}
                ],
                "limit":0}
                """;
        StandardReconConfig config = StandardReconConfig.reconstruct(json);
        assertNull(config.typeID);
        assertNull(config.typeName);
    }

    @Test
    public void testGetBatchSize() throws IOException {

        String json = """
                {"mode":"standard-service",
                "service":"https://tools.wmflabs.org/openrefine-wikidata/en/api",
                "identifierSpace":"http://www.wikidata.org/entity/",
                "schemaSpace":"http://www.wikidata.org/prop/direct/",
                "type":null,
                "autoMatch":true,
                "batchSize":50,
                "columnDetails":[
                    {"column":"_ - id",
                     "property":{"id":"P3153","name":"Crossref funder ID"}
                     }
                ],
                "limit":0}
                """;
        StandardReconConfig c = StandardReconConfig.reconstruct(json);
        assertEquals(c.getBatchSize(10), 10);
        assertEquals(c.getBatchSize(120), 12);
        assertEquals(c.getBatchSize(1200), 50);
        assertEquals(c.getBatchSize(10000), 50);
    }

    @Test
    public void testGetColumnDependencies() throws IOException {
        String json = """
                {"mode":"standard-service",
                "service":"https://tools.wmflabs.org/openrefine-wikidata/en/api",
                "identifierSpace":"http://www.wikidata.org/entity/",
                "schemaSpace":"http://www.wikidata.org/prop/direct/",
                "type":null,
                "autoMatch":true,
                "batchSize":50,
                "columnDetails":[
                    {"column":"_ - id",
                     "property":{"id":"P3153","name":"Crossref funder ID"}}
                ],
                "limit":0}
                """;
        StandardReconConfig c = StandardReconConfig.reconstruct(json);
        assertEquals(c.getColumnDependencies(), Optional.of(Set.of("_ - id")));
    }

    @Test
    public void testReconstructNoIdentifierSchemaSpaces() throws IOException {
        String json = """
                {"mode":"standard-service",
                "service":"https://tools.wmflabs.org/openrefine-wikidata/en/api",
                "type":null,
                "autoMatch":true,
                "columnDetails":[],
                "limit":0}
                """;
        StandardReconConfig config = StandardReconConfig.reconstruct(json);
        assertEquals(config.identifierSpace, "http://localhost/identifier");
        assertEquals(config.schemaSpace, "http://localhost/schema");
    }

    @Test
    public void formulateQueryTest() throws IOException {
        Project project = createProject(
                new String[] { "title", "director" },
                new Serializable[][] {
                        { "mulholland drive", "david lynch" }
                });

        String config = """
                 {
                        "mode": "standard-service",
                        "service": "https://tools.wmflabs.org/openrefine-wikidata/en/api",
                        "identifierSpace": "http://www.wikidata.org/entity/",
                        "schemaSpace": "http://www.wikidata.org/prop/direct/",
                        "type": {
                                "id": "Q1234",
                                "name": "movie"
                        },
                        "autoMatch": true,
                        "columnDetails": [
                           {
                             "column": "director",
                             "propertyName": "Director",
                             "propertyID": "P123"
                           }
                        ]}
                """;
        StandardReconConfig r = StandardReconConfig.reconstruct(config);
        Row row = project.rows.get(0);
        ReconJob job = r.createJob(project, 0, row, "title", row.getCell(0));
        TestUtils.assertEqualsAsJson(job.toString(), """
                {
                "query":"mulholland drive",
                "type":"Q1234",
                "properties":[
                     {"pid":"P123","v":"david lynch"}
                ],
                "type_strict":"should"
                }
                """);
    }

    @Test

    public void batchReconTestSuccessful() throws Exception {
        String reconResponse = """
                {
                q0: {
                  result: [
                    {
                    P57: {
                score: 100,
                weighted: 40
                },
                all_labels: {
                score: 59,
                weighted: 59
                },
                score: 70.71428571428572,
                id: "Q3989262",
                name: "The Short Films of David Lynch",
                type: [
                {
                id: "Q24862",
                name: "short film"
                },
                {
                id: "Q202866",
                name: "animated film"
                }
                ],
                match: false
                },
                {
                P57: {
                score: 100,
                weighted: 40
                },
                all_labels: {
                score: 44,
                weighted: 44
                },
                score: 60.00000000000001,
                id: "Q83365219",
                name: "What Did Jack Do?",
                type: [
                {
                id: "Q24862",
                name: "short film"
                }
                ],
                match: false
                    }
                    ]
                  }
                }
                """;
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/openrefine-wikidata/en/api");
            server.enqueue(new MockResponse.Builder().body(reconResponse).build()); // service returns successfully
            server.enqueue(new MockResponse.Builder().build());

            String configJson = """
                    {
                     "mode": "standard-service",
                     "service": "%s",
                     "identifierSpace": "http://www.wikidata.org/entity/",
                     "schemaSpace": "http://www.wikidata.org/prop/direct/",
                     "type": {
                             "id": "Q11424",
                             "name": "film"
                     },
                     "autoMatch": true,
                     "columnDetails": [
                        {
                          "column": "director",
                          "propertyName": "Director",
                          "propertyID": "P57"
                        }
                     ]}
                    """.formatted(url);
            StandardReconConfig config = StandardReconConfig.reconstruct(configJson);
            StandardReconConfig.StandardReconJob job = new StandardReconConfig.StandardReconJob();
            job.text = QUERY_TEXT;
            job.code = QUERY_JSON;
            List<ReconJob> jobList = new ArrayList<ReconJob>();
            jobList.add(job);
            List<Recon> returnReconList = config.batchRecon(jobList, 1000000000);
            // Check the response we got back
            assertNotNull(returnReconList);
            assertEquals(returnReconList.get(0).getBestCandidate().types[0], "Q24862");

            // Client has already received the response that we queued earlier,
            // but look at the request that we got to see if it was formatted correctly
            RecordedRequest request1 = server.takeRequest();
            assertNotNull(request1);
            assertNotNull(request1.getBody());
            String query = request1.getBody().utf8();
            String expected = "queries=" + URLEncoder.encode(
                    "{\"q0\":" + QUERY_JSON + "}",
                    StandardCharsets.UTF_8);
            assertEquals(query, expected);

        }
    }

    @Test
    public void batchReconTestError() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/openrefine-wikidata/en/api");
            server.enqueue(new MockResponse.Builder().code(500).build());

            String configJson = """
                    {
                     "mode": "standard-service",
                     "service": "%s",
                     "identifierSpace": "http://www.wikidata.org/entity/",
                     "schemaSpace": "http://www.wikidata.org/prop/direct/",
                     "type": {
                             "id": "Q11424",
                             "name": "film"
                     },
                     "autoMatch": true,
                     "columnDetails": [
                        {
                          "column": "director",
                          "propertyName": "Director",
                          "propertyID": "P57"
                        }
                     ]}
                    """.formatted(url);
            StandardReconConfig config = StandardReconConfig.reconstruct(configJson);
            StandardReconConfig.StandardReconJob job = new StandardReconConfig.StandardReconJob();
            job.text = QUERY_TEXT;
            job.code = QUERY_JSON;
            List<ReconJob> jobList = new ArrayList<>();
            jobList.add(job);

            // calling the batchRecon
            List<Recon> returnReconList = config.batchRecon(jobList, 1000000000);

            RecordedRequest request1 = server.takeRequest();
            assertNotNull(request1);
            assertNotNull(request1.getBody());
            String query = request1.getBody().utf8();
            String expected = "queries=" + URLEncoder.encode(
                    "{\"q0\":" + QUERY_JSON + "}",
                    StandardCharsets.UTF_8);

            // assertions

            assertEquals(query, expected);
            assertNotNull(returnReconList);
            assertNotNull(returnReconList.get(0));
            assertNotNull(returnReconList.get(0).error);
            // checking for error due to missing result field
            String reconResponse = """
                    {
                      q0: {
                      }
                    }
                    """;
            server.enqueue(new MockResponse.Builder().body(reconResponse).build()); // service returns successfully
            returnReconList = config.batchRecon(jobList, 1000000000);
            assertEquals(query, expected);
            assertNotNull(returnReconList.get(0));
            assertNotNull(returnReconList.get(0).error);
            assertEquals(returnReconList.get(0).error, "The service returned a JSON response without \"result\" field for query q0");
        }
    }

    @Test
    public void batchReconTestConnectionError() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/openrefine-wikidata/en/api");
            // enqueue a few responses to cater for the automatic retries
            server.enqueue(new MockResponse.Builder().onResponseStart(new SocketEffect.CloseSocket()).build());
            server.enqueue(new MockResponse.Builder().onResponseStart(new SocketEffect.CloseSocket()).build());
            server.enqueue(new MockResponse.Builder().onResponseStart(new SocketEffect.CloseSocket()).build());
            server.enqueue(new MockResponse.Builder().onResponseStart(new SocketEffect.CloseSocket()).build());

            String configJson = """
                    {
                    "mode": "standard-service",
                    "service": "%s",
                    "identifierSpace": "http://www.wikidata.org/entity/",
                    "schemaSpace": "http://www.wikidata.org/prop/direct/",
                    "type": {
                            "id": "Q11424",
                            "name": "film"
                    },
                    "autoMatch": true,
                    "columnDetails": [
                       {
                         "column": "director",
                         "propertyName": "Director",
                         "propertyID": "P57"
                       }
                    ]}
                    """.formatted(url);
            StandardReconConfig config = StandardReconConfig.reconstruct(configJson);
            StandardReconConfig.StandardReconJob job = new StandardReconConfig.StandardReconJob();
            job.text = QUERY_TEXT;
            job.code = QUERY_JSON;
            List<ReconJob> jobList = new ArrayList<>();
            jobList.add(job);

            // calling the batchRecon
            List<Recon> returnReconList = config.batchRecon(jobList, 1000000000);

            RecordedRequest request1 = server.takeRequest();
            assertNotNull(request1);

            // the error message is unstable and system-dependent, so we are not asserting for its exact contents.
            assertNotNull(returnReconList.get(0).error);
        }
    }

    @Test
    public void batchReconTestDNSError() throws Exception {
        HttpUrl url = HttpUrl.parse("https://hewsjsajsajk.com/search?q=ujdjsaoiksa");

        String configJson = """
                {
                 "mode": "standard-service",
                 "service": "%s",
                 "identifierSpace": "http://www.wikidata.org/entity/",
                 "schemaSpace": "http://www.wikidata.org/prop/direct/",
                 "type": {
                         "id": "Q11424",
                         "name": "film"
                 },
                 "autoMatch": true,
                 "columnDetails": [
                    {
                      "column": "director",
                      "propertyName": "Director",
                      "propertyID": "P57"
                    }
                 ]}
                """.formatted(url);
        StandardReconConfig config = StandardReconConfig.reconstruct(configJson);
        StandardReconConfig.StandardReconJob job = new StandardReconConfig.StandardReconJob();
        job.text = QUERY_TEXT;
        job.code = QUERY_JSON;
        List<ReconJob> jobList = new ArrayList<>();
        jobList.add(job);

        List<Recon> returnReconList = config.batchRecon(jobList, 1000000000);
        assertNotNull(returnReconList);
        assertNotNull(returnReconList.get(0).error);
        // the error message is unstable and system-dependent, so we are not asserting for its exact contents.

    }

    /**
     * The UI format and the backend format differ for serialization (the UI never deserializes and the backend
     * serialization did not matter). TODO: change the frontend so it uses the same format.
     */
    @Test
    public void deserializeColumnDetail() throws JsonParseException, JsonMappingException, IOException {
        String uiJson = """
                {"column":"director",
                "property":{
                   "id":"P123",
                   "name":"Director"
                }}
                """;
        String backendJson = """
                {"column":"director",
                "propertyID":"P123",
                "propertyName":"Director"}
                """;
        ColumnDetail cd = ParsingUtilities.mapper.readValue(uiJson, ColumnDetail.class);
        TestUtils.isSerializedTo(cd, backendJson);
    }

    @Test
    public void deserializeReconResult() throws JsonParseException, JsonMappingException, IOException {
        String json = """
                {"score":100.0,
                "match":false,
                "type":[
                   {"id":"Q17366755",
                    "name":"hamlet in Alberta"}],
                "id":"Q5136635",
                "name":"Cluny"}
                """;
        ReconResult rr = ParsingUtilities.mapper.readValue(json, ReconResult.class);
        assertEquals(rr.types.get(0).name, "hamlet in Alberta");
    }

    // Issue #1913
    @Test
    public void reorderReconciliationResults() throws JsonParseException, JsonMappingException, IOException {
        String viafJson = """
                 [
                    {
                        "id": "18951129",
                        "name": "Varano, Camilla Battista da 1458-1524",
                        "type": [
                            {
                                "id": "/people/person",
                                "name": "Person"
                            }
                        ],
                        "score": 0.1282051282051282,
                        "match": false
                    },
                    {
                        "id": "102271932",
                        "name": "Shamsie, Kamila, 1973-....",
                        "type": [
                            {
                                "id": "/people/person",
                                "name": "Person"
                            }
                        ],
                        "score": 0.23076923076923078,
                        "match": false
                    },
                    {
                        "id": "63233597",
                        "name": "Camilla, Duchess of Cornwall, 1947-",
                        "type": [
                            {
                                "id": "/people/person",
                                "name": "Person"
                            }
                        ],
                        "score": 0.14285714285714285,
                        "match": false
                    }
                ]""";

        StandardReconConfigStub stub = new StandardReconConfigStub();
        ArrayNode node = ParsingUtilities.mapper.readValue(viafJson, ArrayNode.class);
        Recon recon = stub.createReconServiceResults("Kamila", node, 1234L);
        assertTrue(recon.candidates.get(0).score > 0.2);
        assertEquals(recon.candidates.get(0).id, "102271932");
    }

    @Test
    public void reorderReconciliationResultsStableSort() throws JsonParseException, JsonMappingException, IOException {
        String viafJson = """
                 [
                    {
                        "id": "18951129",
                        "name": "Varano, Camilla Battista da 1458-1524",
                        "type": [
                            {
                                "id": "/people/person",
                                "name": "Person"
                            }
                        ],
                        "score": 0.3,
                        "match": false
                    },
                    {
                        "id": "102271932",
                        "name": "Shamsie, Kamila, 1973-....",
                        "type": [
                            {
                                "id": "/people/person",
                                "name": "Person"
                            }
                        ],
                        "score": 0.23076923076923078,
                        "match": false
                    },
                    {
                        "id": "63233597",
                        "name": "Camilla, Duchess of Cornwall, 1947-",
                        "type": [
                            {
                                "id": "/people/person",
                                "name": "Person"
                            }
                        ],
                        "score": 0.3,
                        "match": false
                    }
                ]
                """;

        StandardReconConfigStub stub = new StandardReconConfigStub();
        ArrayNode node = ParsingUtilities.mapper.readValue(viafJson, ArrayNode.class);
        Recon recon = stub.createReconServiceResults("Kamila", node, 1234L);
        assertEquals(recon.candidates.get(0).score, 0.3);
        assertEquals(recon.candidates.get(0).id, "18951129");
    }

    /**
     * computing the features on an empty recon should not fail
     */
    @Test
    public void testComputeFeatures() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        Recon recon = stub.createNewRecon(2384738L);
        stub.computeFeatures(recon, "my string");
        assertNotNull(recon.features);
    }

    /**
     * Should not happen, but added for extra safety
     */
    @Test
    public void testComputeFeaturesNullText() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        Recon recon = stub.createNewRecon(2384738L);
        stub.computeFeatures(recon, null);
        assertNotNull(recon.features);
    }

    @Test
    public void testBreakWordsUsesStopWordsPreference() {
        ProjectManager.singleton.getPreferenceStore()
                .put("stopwords", "the,a,and,of,on,in,at,by,le");
        Set<String> words = StandardReconConfig.breakWords("Le Petit Prince");
        Assert.assertEquals(Set.of("petit", "prince"), words);
        Assert.assertFalse(words.contains("le"));
        Assert.assertTrue(words.contains("petit"));
        Assert.assertTrue(words.contains("prince"));
    }

    @Test
    public void testBreakWordsUsesStopWordsTrimWhitespace() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        ProjectManager.singleton.getPreferenceStore()
                .put("stopwords", "the, a, and, of, on,in,at,by, le");
        Set<String> words = stub.breakWords("Le Petit Prince");
        Assert.assertEquals(Set.of("petit", "prince"), words);
        Assert.assertFalse(words.contains("le"));
        Assert.assertTrue(words.contains("petit"));
        Assert.assertTrue(words.contains("prince"));
    }

    @Test
    public void testBreakWordsDefaultStopWords() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        ProjectManager.singleton.getPreferenceStore()
                .put("stopwords", null);
        Set<String> words = stub.breakWords("The Lord of the Rings");
        Assert.assertEquals(Set.of("lord", "rings"), words);
        Assert.assertFalse(words.contains("the"));
        Assert.assertFalse(words.contains("of"));
        Assert.assertTrue(words.contains("lord"));
        Assert.assertTrue(words.contains("rings"));
    }
}
