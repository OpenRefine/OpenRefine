
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String modname = request.getParameter("module");
        if (modname == null) {
            modname = "core";
        }

        String[] langs = request.getParameterValues("lang");
        if (langs == null || "".equals(langs[0])) {
            PreferenceStore ps = ProjectManager.singleton.getPreferenceStore();
            if (ps != null) {
                langs = new String[] {(String) ps.get("userLang")};
            }
        }
        
        // Default language is English
        langs = Arrays.copyOf(langs, langs.length+1);
        langs[langs.length-1] = "en";

        ObjectNode json = null;
        boolean loaded = false;
        for (String lang : langs) {
            if (lang == null) continue;
            json = loadLanguage(this.servlet, modname, lang);
            if (json != null) {
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                try {
                    ObjectNode node = ParsingUtilities.mapper.createObjectNode();
                    node.put("dictionary", json);
                    node.put("lang", new TextNode(lang));
                	ParsingUtilities.mapper.writeValue(response.getWriter(), node);
                } catch (IOException e) {
                    logger.error("Error writing language labels to response stream");
                }
                response.getWriter().flush();
                response.getWriter().close();
                loaded = true;
                break;
            }
        }
        if (!loaded) {
        	logger.error("Failed to load any language files");
        }
    }
    
    static ObjectNode loadLanguage(RefineServlet servlet, String modname, String lang) throws UnsupportedEncodingException {
        
        ButterflyModule module = servlet.getModule(modname);
        File langFile = new File(module.getPath(), "langs" + File.separator + "translation-" + lang + ".json");
        try {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(langFile), "UTF-8"));
            return ParsingUtilities.mapper.readValue(reader, ObjectNode.class);
        } catch (FileNotFoundException e1) {
            // Could be normal if we've got a list of languages as fallbacks
        } catch (IOException e) {
            logger.error("JSON error reading/writing language file: " + langFile, e);
        }
        return null;
    }
}
