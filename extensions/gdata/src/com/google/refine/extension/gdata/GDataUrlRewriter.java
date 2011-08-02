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

import com.google.refine.importing.UrlRewriter;

/**
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
public class GDataUrlRewriter implements UrlRewriter {

    @Override
    public Result rewrite(String urlString) {
        try {
            URL url = new URL(urlString);
            if (isSpreadsheetURL(url)) {
                Result result = new Result();
                result.rewrittenUrl = urlString;
                result.format = "service/gdata/spreadsheet";
                result.download = false;
                return result;
            }
        } catch (MalformedURLException e) {
            // Ignore
        }
        return null;
    }
    
    static public boolean isSpreadsheetURL(URL url) {
        String host = url.getHost();
        String query = url.getQuery();
        if (query == null) {
            query = "";
        }
        // http://spreadsheets.google.com/ccc?key=tI36b9Fxk1lFBS83iR_3XQA&hl=en
        return host.endsWith(".google.com") && host.contains("spreadsheet") && query.contains("key=");
    }
}
