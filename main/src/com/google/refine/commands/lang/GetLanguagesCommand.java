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

package com.google.refine.commands.lang;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.commands.Command;

import edu.mit.simile.butterfly.ButterflyModule;

public class GetLanguagesCommand extends Command {
    
    public GetLanguagesCommand() {
        super();
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String modname = request.getParameter("module");
        if (modname == null) {
            modname = "core";
        }
        
        ButterflyModule module = this.servlet.getModule(modname);

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            writer.key("languages");
            writer.array();
            writeLangData(writer, "en", "English"); // we always have English and it's always first
            
            FileFilter fileFilter = new WildcardFileFilter("translation-*.json");
            for (File file : new File(module.getPath() + File.separator + "langs").listFiles(fileFilter)) {
                String lang = file.getName().split("-")[1].split("\\.")[0];
                if (!"en".equals(lang) && !"default".equals(lang)) {
                    JSONObject json = LoadLanguageCommand.loadLanguage(this.servlet, "core", lang);
                    if (json != null) {
                        String label = json.getString("name");
                        writeLangData(writer, lang, label);
                    }
                }
            }

            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

    private void writeLangData(JSONWriter writer, String lang, String label)
            throws JSONException {
        writer.object();
        writer.key("code"); writer.value(lang);
        writer.key("label"); writer.value(label);
        writer.endObject();
    }

}
