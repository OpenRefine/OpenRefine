/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.commands.recon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconType;
import com.google.refine.model.Row;
import com.google.refine.model.recon.StandardReconConfig.ReconResult;
import com.google.refine.util.HttpClient;
import com.google.refine.util.ParsingUtilities;

public class GuessTypesOfColumnCommand extends Command {

    final static int DEFAULT_SAMPLE_SIZE = 10;
    private int sampleSize = DEFAULT_SAMPLE_SIZE;

    protected static class TypesResponse {

        @JsonProperty("code")
        protected String code;
        @JsonProperty("message")
        @JsonInclude(Include.NON_NULL)
        protected String message;
        @JsonProperty("types")
        @JsonInclude(Include.NON_NULL)
        List<TypeGroup> types;

        protected TypesResponse(
                String code,
                String message,
                List<TypeGroup> types) {
            this.code = code;
            this.message = message;
            this.types = types;
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");
            String serviceUrl = request.getParameter("service");

            Column column = project.columnModel.getColumnByName(columnName);
            if (column == null) {
                respondJSON(response, new TypesResponse("error", "No such column", null));
            } else {
                List<TypeGroup> typeGroups = guessTypes(project, column, serviceUrl);
                respondJSON(response, new TypesResponse("ok", null, typeGroups));
            }

        } catch (Exception e) {
            respondException(response, e);
        }
    }

    protected static class IndividualQuery {

        @JsonProperty("query")
        protected String query;
        @JsonProperty("limit")
        protected int limit;

        protected IndividualQuery(String query, int limit) {
            this.query = query;
            this.limit = limit;
        }
    }

    /**
     * Run relevance searches for the first n cells in the given column and count the types of the results. Return a
     * sorted list of types, from most frequent to least.
     * 
     * @param project
     * @param column
     * @return
     * @throws IOException
     */
    protected List<TypeGroup> guessTypes(Project project, Column column, String serviceUrl)
            throws IOException {
        Map<String, TypeGroup> map = new HashMap<String, TypeGroup>();

        int cellIndex = column.getCellIndex();

        List<String> samples = new ArrayList<String>(sampleSize);
        Set<String> sampleSet = new HashSet<String>();

        for (Row row : project.rows) {
            Object value = row.getCellValue(cellIndex);
            if (ExpressionUtils.isNonBlankData(value)) {
                String s = CharMatcher.whitespace().trimFrom(value.toString());
                if (!sampleSet.contains(s)) {
                    samples.add(s);
                    sampleSet.add(s);
                    if (samples.size() >= sampleSize) {
                        break;
                    }
                }
            }
        }

        Map<String, IndividualQuery> queryMap = new HashMap<>();
        for (int i = 0; i < samples.size(); i++) {
            queryMap.put("q" + i, new IndividualQuery(samples.get(i), 3));
        }

        String queriesString = ParsingUtilities.defaultWriter.writeValueAsString(queryMap);
        String responseString;
        try {
            responseString = postQueries(serviceUrl, queriesString);
            ObjectNode o = ParsingUtilities.evaluateJsonStringToObjectNode(responseString);

            Iterator<JsonNode> iterator = o.iterator();
            while (iterator.hasNext()) {
                JsonNode o2 = iterator.next();
                if (!(o2.has("result") && o2.get("result") instanceof ArrayNode)) {
                    continue;
                }

                ArrayNode results = (ArrayNode) o2.get("result");
                List<ReconResult> reconResults = ParsingUtilities.mapper.convertValue(results, new TypeReference<List<ReconResult>>() {
                });
                int count = reconResults.size();

                for (int j = 0; j < count; j++) {
                    ReconResult result = reconResults.get(j);
                    double score = 1.0 / (1 + j); // score by each result's rank

                    List<ReconType> types = result.types;
                    int typeCount = types.size();

                    for (int t = 0; t < typeCount; t++) {
                        ReconType type = types.get(t);
                        double score2 = score * (typeCount - t) / typeCount;
                        if (map.containsKey(type.id)) {
                            TypeGroup tg = map.get(type.id);
                            tg.score += score2;
                            tg.count++;
                        } else {
                            map.put(type.id, new TypeGroup(type.id, type.name, score2));
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to guess cell types for load\n" + queriesString, e);
            throw e;
        }

        List<TypeGroup> types = new ArrayList<TypeGroup>(map.values());
        Collections.sort(types, new Comparator<TypeGroup>() {

            @Override
            public int compare(TypeGroup o1, TypeGroup o2) {
                int c = Math.min(sampleSize, o2.count) - Math.min(sampleSize, o1.count);
                if (c != 0) {
                    return c;
                }
                return (int) Math.signum(o2.score / o2.count - o1.score / o1.count);
            }
        });

        return types;
    }

    private String postQueries(String serviceUrl, String queriesString) throws IOException {
        HttpClient client = new HttpClient();
        return client.postNameValue(serviceUrl, "queries", queriesString);
    }

    static protected class TypeGroup {

        @JsonProperty("id")
        protected String id;
        @JsonProperty("name")
        protected String name;
        @JsonProperty("count")
        protected int count;
        @JsonProperty("score")
        protected double score;

        TypeGroup(String id, String name, double score) {
            this.id = id;
            this.name = name;
            this.score = score;
            this.count = 1;
        }
    }

    // for testability
    protected void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }
}
