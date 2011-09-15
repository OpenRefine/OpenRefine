/*
 * Copyright (c) 2010, Thomas F. Morris
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
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
abstract public class GDataExtension {
    static final String SERVICE_APP_NAME = "Google-Refine-GData-Extension";

    static private FeedURLFactory factory;
    static public FeedURLFactory getFeedUrlFactory() {
        if (factory == null) {
            // Careful - this is shared by everyone.
            factory = FeedURLFactory.getDefault();
        }
        return factory;
    }
    
    static public DocsService getDocsService(String token) {
        DocsService service = new DocsService(SERVICE_APP_NAME);
        service.setAuthSubToken(token);
        return service;
    }
    
    static public SpreadsheetService getSpreadsheetService(String token) {
        SpreadsheetService service = new SpreadsheetService(SERVICE_APP_NAME);
        service.setAuthSubToken(token);
        return service;
    }
    
    static public GoogleService getFusionTablesGoogleService(String token) {
        GoogleService service = new GoogleService("fusiontables", SERVICE_APP_NAME);
        service.setAuthSubToken(token);
        return service;
    }

    final static private String FUSION_TABLES_SERVICE_URL =
        "https://www.google.com/fusiontables/api/query";

    final static private Pattern CSV_VALUE_PATTERN =
        Pattern.compile("([^,\\r\\n\"]*|\"(([^\"]*\"\")*[^\"]*)\")(,|\\r?\\n)");
    
    static public List<List<String>> runFusionTablesSelect(GoogleService service, String selectQuery)
            throws IOException, ServiceException {
        
        URL url = new URL(FUSION_TABLES_SERVICE_URL + "?sql=" +
                URLEncoder.encode(selectQuery, "UTF-8"));
        GDataRequest request = service.getRequestFactory().getRequest(
                RequestType.QUERY, url, ContentType.TEXT_PLAIN);
        
        request.execute();
        
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
        if (row != null) {
            rows.add(row);
        }
        return rows;
    }
}
