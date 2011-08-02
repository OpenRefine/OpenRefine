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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;
import com.google.refine.importing.UrlRewriter;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
public class FusionTablesUrlRewriter implements UrlRewriter {

    @Override
    public Result rewrite(String urlString) {
        try {
            URL url = new URL(urlString);
            if (isFusionTableURL(url)) {
                Result result = new Result();
                try {
                    result.rewrittenUrl = generateQueryUrl(url, 0, -1).toExternalForm();
                    result.format = "text/line-based/*sv";
                    result.download = true;
                    return result;
                } catch (UnsupportedEncodingException e) {
                    // TODO: what do we do here?
                }
            }
        } catch (MalformedURLException e) {
            // Ignore
        }
        return null;
    }
    
    static public boolean isFusionTableURL(URL url) {
        // http://www.google.com/fusiontables/DataSource?dsrcid=1219
        String query = url.getQuery();
        if (query == null) {
            query = "";
        }
        return url.getHost().endsWith(".google.com") 
                && url.getPath().startsWith("/fusiontables/DataSource")
                && query.contains("dsrcid=");
    }
    
    static public URL generateQueryUrl(URL url, int start, int limit)
        throws MalformedURLException, UnsupportedEncodingException {
        
        String tableId = getFusionTableKey(url);
        
        final String SERVICE_URL =
            "http://www.google.com/fusiontables/api/query";
        final String selectQuery = "select * from " + tableId
            + " offset " + (start) + (limit > 0 ? (" limit " + limit) : "");
        
        return new URL(SERVICE_URL + "?sql=" + URLEncoder.encode(selectQuery, "UTF-8"));
    }
    
    static public InputStream openInputStream(URL queryUrl) throws IOException, ServiceException {
        GoogleService service = new GoogleService("fusiontables", GDataExtension.SERVICE_APP_NAME);
        // String token = TokenCookie.getToken(request);
        // if (token != null) {
        // service.setAuthSubToken(token);
        // }
        GDataRequest queryRequest = service.getRequestFactory().getRequest(
                RequestType.QUERY, queryUrl, ContentType.TEXT_PLAIN);
        queryRequest.execute();
        
        return queryRequest.getResponseStream();
    }
    
    static private String getFusionTableKey(URL url) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith("dsrcid=")) {
                    int offset = ("dsrcid=").length();
                    String tableId = part.substring(offset);
                    // TODO: Any special id format considerations to worry about?
    //                if (tableId.startsWith("p") || !tableId.contains(".")) {
    //                    return tableId;
    //                }
                    return tableId;
                }
            }
        }
        return null;
    }
}
