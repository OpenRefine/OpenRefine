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

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.http.AuthSubUtil;
import com.google.gdata.client.spreadsheet.SpreadsheetService;

import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
abstract public class GDataExtension {
    static final String SERVICE_APP_NAME = "OpenRefine-GData-Extension";

    static public String getAuthorizationUrl(ButterflyModule module, HttpServletRequest request)
            throws MalformedURLException {
        char[] mountPointChars = module.getMountPoint().getMountPoint().toCharArray();
    
        StringBuffer sb = new StringBuffer();
        sb.append(mountPointChars, 0, mountPointChars.length);
        sb.append("authorized?winname=");
        sb.append(ParsingUtilities.encode(request.getParameter("winname")));
        sb.append("&callback=");
        sb.append(ParsingUtilities.encode(request.getParameter("callback")));
    
        URL thisUrl = new URL(request.getRequestURL().toString());
        URL authorizedUrl = new URL(thisUrl, sb.toString());
        
        return AuthSubUtil.getRequestUrl(
            authorizedUrl.toExternalForm(), // execution continues at authorized on redirect
            "https://docs.google.com/feeds https://spreadsheets.google.com/feeds",
            false,
            true);
    }


    static public DocsService getDocsService(String token) {
        DocsService service = new DocsService(SERVICE_APP_NAME);
        if (token != null) {
            service.setAuthSubToken(token);
        }
        return service;
    }
    
    static public SpreadsheetService getSpreadsheetService(String token) {
        SpreadsheetService service = new SpreadsheetService(SERVICE_APP_NAME);
        if (token != null) {
            service.setAuthSubToken(token);
        }
        return service;
    }
    
    static boolean isSpreadsheetURL(String url) {
        // e.g. http://spreadsheets.google.com/ccc?key=tI36b9Fxk1lFBS83iR_3XQA&hl=en
        // TODO: The following should work, but the GData implementation is too limited
//        try {
//            FeedURLFactory.getSpreadsheetKeyFromUrl(url);
//            return true;
//        } catch (IllegalArgumentException e) {
//            return false;
//        }
        try {
            return url.contains("spreadsheet") && getSpreadsheetID(new URL(url)) != null;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    static String getSpreadsheetID(URL url) {
        return getParamValue(url,"key");
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
