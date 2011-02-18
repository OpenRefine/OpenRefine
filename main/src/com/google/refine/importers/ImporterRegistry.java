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

package com.google.refine.importers;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract public class ImporterRegistry {
    final static Logger logger = LoggerFactory.getLogger("parser-registry");

    static final private Map<String, Importer> importers = new HashMap<String, Importer>();

    private static final String[][] importerNames = {
        {"ExcelImporter", "com.google.refine.importers.ExcelImporter"},
        {"XmlImporter", "com.google.refine.importers.XmlImporter"},
        {"RdfTripleImporter", "com.google.refine.importers.RdfTripleImporter"},
        {"MarcImporter", "com.google.refine.importers.MarcImporter"},
        {"TsvCsvImporter", "com.google.refine.importers.TsvCsvImporter"},
        {"JsonImporter", "com.google.refine.importers.JsonImporter"},
        {"FixedWidthImporter", "com.google.refine.importers.FixedWidthImporter"}
    };

    static {
        registerImporters(importerNames);
    }

    static public boolean registerImporters(String[][] importers) {
        boolean status = true;
        for (String[] importer : importerNames) {
            String importerName = importer[0];
            String className = importer[1];
            logger.debug("Loading command " + importerName + " class: " + className);
            Importer cmd;
            try {
                // TODO: May need to use the servlet container's class loader here
                cmd = (Importer) Class.forName(className).newInstance();
            } catch (InstantiationException e) {
                logger.error("Failed to load parser class " + className, e);
                status = false;
                continue;
            } catch (IllegalAccessException e) {
                logger.error("Failed to load parser class " + className, e);
                status = false;
                continue;
            } catch (ClassNotFoundException e) {
                logger.error("Failed to load parser class " + className, e);
                status = false;
                continue;
            }
            status |= registerImporter(importerName, cmd);
        }
        return status;
    }

    /**
     * Register a single parser.
     *
     * @param name parser verb for parser
     * @param importerObject object implementing the parser
     * 
     * @return true if parser was loaded and registered successfully
     */
    static public boolean registerImporter(String name, Importer importerObject) {
        if (importers.containsKey(name)) {
            return false;
        }
        importers.put(name, importerObject);
        return true;
    }

    // Currently only for test purposes
    static protected boolean unregisterImporter(String verb) {
        return importers.remove(verb) != null;
    }
    
    static public Importer guessImporter(String contentType, String fileName, boolean provideDefault) {
        for (Importer i : importers.values()){
            if(i.canImportData(contentType, fileName)){
                return i;
            }
        }
        if (provideDefault) {
            return new TsvCsvImporter(); // default
        } else {
            return null;
        }
    }
    
    static public Importer guessImporter(String contentType, String filename) {
        return guessImporter(contentType, filename, true);
    }

    static public Importer guessUrlImporter(URL url) {
        for (Importer importer : importers.values()){
            if (importer instanceof UrlImporter 
                    && ((UrlImporter) importer).canImportData(url)) {
                return importer;
            }
        }
        return null;
    }
}
