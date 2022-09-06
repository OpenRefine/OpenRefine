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

package com.google.refine.commands.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

public class LoadLanguageCommand extends Command {

    public LoadLanguageCommand() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * POST is supported but does not actually change any state so we do not add CSRF protection to it. This ensures
     * existing extensions will not have to be updated to add a CSRF token to their requests (2019-11-10)
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String modname = request.getParameter("module");
        if (modname == null) {
            modname = "core";
        }

        // Suggested languages from request, if given...
        String[] langs = request.getParameterValues("lang");
        if (langs == null) {
            langs = new String[] {};
        }

        // Always replace suggested with user preference language, if available...
        PreferenceStore ps = ProjectManager.singleton.getPreferenceStore();
        if (ps != null) {
            String strLang = (String) ps.get("userLang");
            // If user preference language exists...
            if (!(strLang == null || strLang.isEmpty())) {

                // CORRECTOR...
                // TODO: This code may be removed sometime after the 3.7 release has been circulated.
                if ("jp".equals(strLang)) {
                    strLang = "ja";
                    ps.put("userLang", strLang);
                }
                // End CORRECTOR

                langs = new String[] { strLang };
            }
        }

        // Add default English language as least favored...
        if (langs.length == 0 || langs[langs.length - 1] != "en") {
            langs = Arrays.copyOf(langs, langs.length + 1);
            langs[langs.length - 1] = "en";
        }

        ObjectNode translations = null;
        String bestLang = null;

        // Process from least favored to best language...
        for (int i = langs.length - 1; i >= 0; i--) {
            if (langs[i] == null) continue;

            ObjectNode json = LoadLanguageCommand.loadLanguage(this.servlet, modname, langs[i]);
            if (json == null) continue;

            bestLang = langs[i];
            if (translations == null) {
                translations = json;
            } else {
                translations = LoadLanguageCommand.mergeLanguages(json, translations);
            }
        }

        if (translations != null) {
            try {
                ObjectNode node = ParsingUtilities.mapper.createObjectNode();
                node.set("dictionary", translations);
                node.set("lang", new TextNode(bestLang));
                Command.respondJSON(response, node);
            } catch (IOException e) {
                logger.error("Error writing language labels to response stream");
                Command.respondException(response, e);
            }
        } else {
            logger.error("Failed to load any language files");
            Command.respondException(response, new IOException("No language files"));
        }
    }

    static ObjectNode loadLanguage(RefineServlet servlet, String strModule, String strLang)
            throws UnsupportedEncodingException {
        ButterflyModule module = servlet.getModule(strModule);
        String strLangFile = "translation-" + strLang + ".json";
        String strMessage = "[" + strModule + ":" + strLangFile + "]";
        File langFile = new File(module.getPath(), "langs" + File.separator + strLangFile);
        FileInputStream fisLang = null;

        try {
            fisLang = new FileInputStream(langFile);
        } catch (FileNotFoundException e) {
            // Could be normal if we've got a list of languages as fallbacks
            logger.info("Language file " + strMessage + " not found", e);
        } catch (SecurityException e) {
            logger.error("Language file " + strMessage + " cannot be read (security)", e);
        }
        if (fisLang != null) {
            try {
                Reader reader = new BufferedReader(new InputStreamReader(fisLang, "UTF-8"));
                return ParsingUtilities.mapper.readValue(reader, ObjectNode.class);
            } catch (Exception e) {
                logger.error("Language file " + strMessage + " cannot be read (io)", e);
            }
        }
        return null;
    }

    /**
     * Update the language content to the preferred language, server-side
     * 
     * @param preferred
     *            the JSON translation for the preferred language
     * @param fallback
     *            the JSON translation for the fallback language
     * @return a JSON object where values are from the preferred language if available, and the fallback language
     *         otherwise
     */
    static ObjectNode mergeLanguages(ObjectNode preferred, ObjectNode fallback) {
        ObjectNode results = ParsingUtilities.mapper.createObjectNode();
        Iterator<Entry<String, JsonNode>> iterator = fallback.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();
            String code = entry.getKey();
            JsonNode value = preferred.get(code); // ...new value
            if (value == null) {
                value = entry.getValue(); // ...reuse existing value
            }
            results.set(code, value);
        }
        return results;
    }
}
