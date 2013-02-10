/*
 * Copyright (c) 2013, Thomas F. Morris and other contributors
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010,2013 Thomas F. Morris
 */

public class FusionTableHandler {

    final static private String FUSION_TABLES_SERVICE_URL =
    "https://www.google.com/fusiontables/api/query";
//    "https://www.googleapis.com/fusiontables/v1/query";

    final static private Pattern CSV_VALUE_PATTERN =
            Pattern.compile("([^,\\r\\n\"]*|\"(([^\"]*\"\")*[^\"]*)\")(,|\\r?\\n)");
    
    static public GDataRequest createFusionTablesPostRequest(
            GoogleService service, RequestType requestType, String query)
            throws IOException, ServiceException {
        
        URL url = new URL(FUSION_TABLES_SERVICE_URL);
        GDataRequest request = service.getRequestFactory().getRequest(
            requestType, url, new ContentType("application/x-www-form-urlencoded"));
        
        OutputStreamWriter writer =
            new OutputStreamWriter(request.getRequestStream());
        writer.append("sql=" + URLEncoder.encode(query, "UTF-8") + "&alt=csv");
        writer.flush();
        writer.close();
        
        return request;
    }

    static public GDataRequest createFusionTablesRequest(
            GoogleService service, RequestType requestType, String query)
            throws IOException, ServiceException {
        URL url = new URL(FUSION_TABLES_SERVICE_URL + "?sql=" +
                URLEncoder.encode(query, "UTF-8")+"&alt=csv");
        return service.getRequestFactory().getRequest(
                requestType, url, ContentType.TEXT_PLAIN);
    }

    static String getFusionTableKey(URL url) {
            String tableId = getParamValue(url,"dsrcid");
            // TODO: Any special id format considerations to worry about?
    //        if (tableId.startsWith("p") || !tableId.contains(".")) {
    //            return tableId;
    //        }
            return tableId;
        }

    static public GoogleService getFusionTablesGoogleService(String token) {
        GoogleService service = new GoogleService("fusiontables", GDataExtension.SERVICE_APP_NAME);
        if (token != null) {
            service.setAuthSubToken(token);
        }
        return service;
    }

    static boolean isFusionTableURL(URL url) {
        // http://www.google.com/fusiontables/DataSource?dsrcid=1219
        String query = url.getQuery();
        if (query == null) {
            query = "";
        }
        return url.getHost().endsWith(".google.com") 
                && url.getPath().startsWith("/fusiontables/DataSource")
                && query.contains("dsrcid=");
    }

    static public List<List<String>> parseFusionTablesResults(GDataRequest request) throws IOException {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = null;
        
        Scanner scanner = new Scanner(request.getResponseStream(), "UTF-8");
        while (scanner.hasNextLine()) {
            scanner.findWithinHorizon(CSV_VALUE_PATTERN, 0);
            MatchResult match = scanner.match();
            String quotedString = match.group(2);
            String decoded = quotedString == null ? match.group(1) : quotedString.replaceAll("\"\"", "\"");
            
            if (row == null) {
                row = new ArrayList<String>();
            }
            row.add(decoded);
            
            if (!match.group(4).equals(",")) {
                if (row != null) {
                    rows.add(row);
                    row = null;
                }
            }
        }
        scanner.close();
        if (row != null) {
            rows.add(row);
        }
        return rows;
    }

    static public List<List<String>> listTables(GoogleService service) throws IOException, ServiceException {
        List<List<String>> rows = runFusionTablesSelect(service, "SHOW TABLES");
        // Format is id, name to which we append a link URL based on ID
        if (rows.size() > 1) { // excluding headers
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row.size() >= 2) {
                    String id = row.get(0);
                    row.add("https://www.google.com/fusiontables/DataSource?docid=" + id);
                }
            }
        }
        return rows;
    }

    static public List<List<String>> runFusionTablesSelect(GoogleService service, String selectQuery)
            throws IOException, ServiceException {
        
        GDataRequest request = createFusionTablesRequest(service, RequestType.QUERY, selectQuery);
        request.execute();
        return parseFusionTablesResults(request);
    }

    static private String getParamValue(URL url, String key) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith(key+"=")) {
                    int offset = key.length()+1;
                    String tableId = part.substring(offset);
                    return tableId;
                }
            }
        }
        return null; 
    }
}
