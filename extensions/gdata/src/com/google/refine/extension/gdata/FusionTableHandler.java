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
import java.net.URL;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.Fusiontables.Query.Sql;
import com.google.api.services.fusiontables.Fusiontables.Query.SqlGet;
import com.google.api.services.fusiontables.Fusiontables.Table.ImportRows;
import com.google.api.services.fusiontables.model.FusiontablesImport;
import com.google.api.services.fusiontables.model.Sqlresponse;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010,2013 Thomas F. Morris
 */

public class FusionTableHandler {

    static private Sqlresponse executeQuery (Fusiontables service, String query)
            throws IOException {
        Sql sql = service.query().sql(query);
        Sqlresponse response = sql.execute();
        return response;
    }
    
    static String createTable(Fusiontables service, String name, List<String> columnNames) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE TABLE '");
        sb.append(name);
        sb.append("' (");
        boolean first = true;
        for (String columnName : columnNames) {            
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append("'");
            sb.append(columnName);
            sb.append("': STRING");
        }
        sb.append(")");

        String createQuery = sb.toString();

        Sqlresponse response = executeQuery(service, createQuery);
//        response.getTableId(); // FIXME: Oh wait, there's no such F*ing method!!!
        return getTableId(response);
    }

    private static String getTableId(Sqlresponse response) {
        List<Object> row = response.getRows().get(0);
        int i = 0;
        for (String colname : response.getColumns()) {
            if ("tableid".equals(colname)) {
                return (String) row.get(i);
            }
        }
        return null;
    }
    

    /**
     * Insert a set of rows and optionally return the IDs of the new rows.
     * 
     * @param service a Fusiontables object
     * @param sql SQL statement to do the inserts
     * @param returnIds true to return the IDs of the newly inserted rows
     * @return
     * @throws IOException
     */
    static Long insertRows(Fusiontables service, String tableId, AbstractInputStreamContent mediaContent) throws IOException {
        ImportRows importRows = service.table().importRows(tableId, mediaContent);
        importRows.setIsStrict(false);
        FusiontablesImport response = importRows.execute();
        return response.getNumRowsReceived();
    }


    static String getFusionTableKey(URL url) {
            String tableId = getParamValue(url,"dsrcid"); // old style phased out
            if (tableId == null || tableId.isEmpty()) {
                tableId = getParamValue(url,"docid");
            }
            return tableId;
        }

    static public Fusiontables getFusionTablesService(String token) {
        Credential credential = new GoogleCredential().setAccessToken(token);
        Fusiontables fusiontables = new Fusiontables.Builder(
                GoogleAPIExtension.HTTP_TRANSPORT, GoogleAPIExtension.JSON_FACTORY, credential)
        .setApplicationName(GoogleAPIExtension.SERVICE_APP_NAME)
        .build();;
        
        return fusiontables;
    }

    static boolean isFusionTableURL(URL url) {
        // http://www.google.com/fusiontables/DataSource?dsrcid=1219
        String query = url.getQuery();
        if (query == null) {
            query = "";
        }
        return url.getHost().endsWith(".google.com") 
                && url.getPath().startsWith("/fusiontables/DataSource")
                && (query.contains("dsrcid=")||query.contains("docid="));
    }

    static Sqlresponse runFusionTablesSelect(Fusiontables service, String selectQuery)
            throws IOException {
        
        // FIXME:  alt=csv doesn't actually work! It will attempt to parse response as JSON and die
        // perhaps use .executeUnparsed() would work?
        SqlGet query = service.query().sqlGet(selectQuery);//.setAlt("csv"); 
        Sqlresponse response = query.execute();
        return response;
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
