/*
 * Copyright (c) 2017, Tony Opara
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

package com.google.refine.extension.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.ProjectManager;
import com.google.refine.io.FileProjectManager;

public class DatabaseUtils {

    private static final Logger logger = LoggerFactory.getLogger("DatabaseUtils");

    public final static String DATABASE_EXTENSION_DIR = "dbextension";
    public final static String SETTINGS_FILE_NAME = ".saved-db-connections.json";
    public final static String SAVED_CONNECTION_KEY = "savedConnections";

    private static SimpleTextEncryptor textEncryptor = new SimpleTextEncryptor("Aa1Gb@tY7_Y");

    public static int getSavedConnectionsSize() {
        List<DatabaseConfiguration> scList = getSavedConnections();
        if (scList == null || scList.isEmpty()) {
            return 0;
        }

        return scList.size();
    }

    /**
     * GET saved connections
     * 
     * @return
     */
    public static List<DatabaseConfiguration> getSavedConnections() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String filename = getExtensionFilePath();

            File file = new File(filename);
            if (!file.exists()) {
                // logger.debug("saved connections file not found, creating new: {}", filename);

                String dirPath = getExtensionFolder();
                File dirFile = new File(dirPath);
                boolean dirExists = true;
                if (!dirFile.exists()) {
                    dirExists = dirFile.mkdir();
                }

                if (dirExists) {

                    SavedConnectionContainer sc = new SavedConnectionContainer(new ArrayList<DatabaseConfiguration>());
                    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), sc);
                    return sc.getSavedConnections();
                    // return decryptAll(sc.getSavedConnections());

                }

            }
            // logger.debug("saved connections file found {}", filename);
            SavedConnectionContainer savedConnectionContainer = mapper.readValue(new File(filename), SavedConnectionContainer.class);
            // return decryptAll(savedConnectionContainer.getSavedConnections());
            return savedConnectionContainer.getSavedConnections();

        } catch (JsonParseException e) {
            logger.error("JsonParseException: {}", e);
        } catch (JsonMappingException e) {
            logger.error("JsonMappingException: {}", e);
        } catch (IOException e) {
            logger.error("IOException: {}", e);
        }
        return null;
    }

    /**
     * GET one saved connection
     * 
     * @param connectionName
     * @return
     */
    public static DatabaseConfiguration getSavedConnection(String connectionName) {
        // logger.debug("get saved connection called with connectionName: {}", connectionName);
        List<DatabaseConfiguration> savedConfigurations = getSavedConnections();

        for (DatabaseConfiguration dc : savedConfigurations) {
            // logger.debug("Saved Connection : {}", dc.getConnectionName());
            if (dc.getConnectionName().equalsIgnoreCase(connectionName.trim())) {
                // logger.debug("Saved Connection Found : {}", dc);
                // dc.setDatabasePassword(decrypt(dc.getDatabasePassword()));
                return dc;
            }
        }

        return null;
    }

    public static String encrypt(String plainPassword) {
        return textEncryptor.encrypt(plainPassword);
    }

    public static String decrypt(String encodedPassword) {
        return textEncryptor.decrypt(encodedPassword);
    }

    public static List<DatabaseConfiguration> decryptAll(List<DatabaseConfiguration> savedConnections) {
        List<DatabaseConfiguration> dbConfigs = new ArrayList<DatabaseConfiguration>(savedConnections.size());

        for (DatabaseConfiguration d : savedConnections) {
            d.setDatabasePassword(decrypt(d.getDatabasePassword()));
            dbConfigs.add(d);

        }
        return dbConfigs;
    }

    /**
     * ADD to saved connections
     * 
     * @param dbConfig
     */
    public static void addToSavedConnections(DatabaseConfiguration dbConfig) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            String savedConnectionFile = getExtensionFilePath();
            SavedConnectionContainer savedConnectionContainer = mapper.readValue(new File(savedConnectionFile),
                    SavedConnectionContainer.class);
            savedConnectionContainer.getSavedConnections().add(dbConfig);

            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(savedConnectionFile), savedConnectionContainer);

        } catch (JsonGenerationException e1) {
            logger.error("JsonGenerationException: {}", e1);
            // e1.printStackTrace();
        } catch (JsonMappingException e1) {
            logger.error("JsonMappingException: {}", e1);
            // e1.printStackTrace();
        } catch (IOException e1) {
            logger.error("IOException: {}", e1);
            // e1.printStackTrace();
        }
    }

    public static void deleteAllSavedConnections() {
        if (logger.isDebugEnabled()) {
            logger.debug("delete All Saved Connections called...");
        }

        try {

            List<DatabaseConfiguration> savedConnections = getSavedConnections();
            if (logger.isDebugEnabled()) {
                logger.debug("Size before delete SavedConnections :: {}", savedConnections.size());
            }

            ArrayList<DatabaseConfiguration> newSavedConns = new ArrayList<DatabaseConfiguration>();

            ObjectMapper mapper = new ObjectMapper();
            String savedConnectionFile = getExtensionFilePath();
            SavedConnectionContainer savedConnectionContainer = mapper.readValue(new File(savedConnectionFile),
                    SavedConnectionContainer.class);
            savedConnectionContainer.setSavedConnections(newSavedConns);

            if (logger.isDebugEnabled()) {
                logger.debug("Size after delete SavedConnections :: {}", savedConnectionContainer.getSavedConnections().size());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(savedConnectionFile), savedConnectionContainer);

        } catch (JsonGenerationException e1) {
            logger.error("JsonGenerationException: {}", e1);
            // e1.printStackTrace();
        } catch (JsonMappingException e1) {
            logger.error("JsonMappingException: {}", e1);
            // e1.printStackTrace();
        } catch (IOException e1) {
            logger.error("IOException: {}", e1);
            // e1.printStackTrace();
        }

    }

    /**
     * DELETE saved connections
     * 
     * @param connectionName
     */
    public static void deleteSavedConnections(String connectionName) {
        if (logger.isDebugEnabled()) {
            logger.debug("deleteSavedConnections called with: {}", connectionName);
        }

        try {

            List<DatabaseConfiguration> savedConnections = getSavedConnections();
            ;
            if (logger.isDebugEnabled()) {
                logger.debug("Size before delete SavedConnections :: {}", savedConnections.size());
            }

            ArrayList<DatabaseConfiguration> newSavedConns = new ArrayList<DatabaseConfiguration>();
            for (DatabaseConfiguration dc : savedConnections) {
                if (!dc.getConnectionName().equalsIgnoreCase(connectionName.trim())) {
                    newSavedConns.add(dc);
                }

            }

            ObjectMapper mapper = new ObjectMapper();
            String savedConnectionFile = getExtensionFilePath();
            SavedConnectionContainer savedConnectionContainer = mapper.readValue(new File(savedConnectionFile),
                    SavedConnectionContainer.class);
            savedConnectionContainer.setSavedConnections(newSavedConns);

            if (logger.isDebugEnabled()) {
                logger.debug("Size after delete SavedConnections :: {}", savedConnectionContainer.getSavedConnections().size());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(savedConnectionFile), savedConnectionContainer);

        } catch (JsonGenerationException e1) {
            logger.error("JsonGenerationException: {}", e1);
            // e1.printStackTrace();
        } catch (JsonMappingException e1) {
            logger.error("JsonMappingException: {}", e1);
            // e1.printStackTrace();
        } catch (IOException e1) {
            logger.error("IOException: {}", e1);
            // e1.printStackTrace();
        }
    }

    /**
     * EDIT saved connections
     * 
     * @param jdbcConfig
     */
    public static void editSavedConnections(DatabaseConfiguration jdbcConfig) {
        if (logger.isDebugEnabled()) {
            logger.debug("Edit SavedConnections called with: {}", jdbcConfig);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String savedConnectionFile = getExtensionFilePath();
            SavedConnectionContainer savedConnectionContainer = mapper.readValue(new File(savedConnectionFile),
                    SavedConnectionContainer.class);

            List<DatabaseConfiguration> savedConnections = savedConnectionContainer.getSavedConnections();

            ListIterator<DatabaseConfiguration> savedConnArrayIter = (ListIterator<DatabaseConfiguration>) savedConnections.listIterator();

            while (savedConnArrayIter.hasNext()) {
                DatabaseConfiguration sc = (DatabaseConfiguration) savedConnArrayIter.next();

                if (sc.getConnectionName().equals(jdbcConfig.getConnectionName())) {
                    savedConnArrayIter.remove();
                }

            }

            savedConnections.add(jdbcConfig);
            savedConnectionContainer.setSavedConnections(savedConnections);

            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(savedConnectionFile), savedConnectionContainer);

        } catch (JsonGenerationException e1) {
            logger.error("JsonGenerationException: {}", e1);
            e1.printStackTrace();
        } catch (JsonMappingException e1) {
            logger.error("JsonMappingException: {}", e1);
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.error("IOException: {}", e1);
            e1.printStackTrace();
        }
    }

    public static String getExtensionFilePath() {
        File dir = ((FileProjectManager) ProjectManager.singleton).getWorkspaceDir();
        String fileSep = System.getProperty("file.separator");
        String filename = dir.getPath() + fileSep + DATABASE_EXTENSION_DIR + fileSep + SETTINGS_FILE_NAME;

        logger.debug("** extension file name: {} **", filename);
        return filename;
    }

    public static String getExtensionFolder() {
        File dir = ((FileProjectManager) ProjectManager.singleton).getWorkspaceDir();
        String fileSep = System.getProperty("file.separator");
        String filename = dir.getPath() + fileSep + DATABASE_EXTENSION_DIR;
        return filename;
    }

    public static DatabaseColumnType getDbColumnType(int dbColumnType) {

        switch (dbColumnType) {
            case java.sql.Types.BIGINT:
                return DatabaseColumnType.NUMBER;
            case java.sql.Types.FLOAT:
                return DatabaseColumnType.FLOAT;
            case java.sql.Types.REAL:
                return DatabaseColumnType.DOUBLE;
            case java.sql.Types.DOUBLE:
                return DatabaseColumnType.DOUBLE;
            case java.sql.Types.NUMERIC:
                return DatabaseColumnType.NUMBER;
            case java.sql.Types.DECIMAL:
                return DatabaseColumnType.STRING;
            case java.sql.Types.CHAR:
                return DatabaseColumnType.STRING;
            case java.sql.Types.VARCHAR:
                return DatabaseColumnType.STRING;
            case java.sql.Types.LONGVARCHAR:
                return DatabaseColumnType.STRING;
            case java.sql.Types.DATE:
                return DatabaseColumnType.DATE;
            case java.sql.Types.TIME:
                return DatabaseColumnType.DATETIME;
            case java.sql.Types.TIMESTAMP:
                return DatabaseColumnType.DATETIME;
            case java.sql.Types.BINARY:
                return DatabaseColumnType.STRING;
            case java.sql.Types.VARBINARY:
                return DatabaseColumnType.STRING;
            case java.sql.Types.LONGVARBINARY:
                return DatabaseColumnType.STRING;
            case java.sql.Types.NULL:
                return DatabaseColumnType.STRING;
            case java.sql.Types.OTHER:
                return DatabaseColumnType.STRING;
            case java.sql.Types.JAVA_OBJECT:
                return DatabaseColumnType.STRING;
            case java.sql.Types.DISTINCT:
                return DatabaseColumnType.STRING;
            case java.sql.Types.STRUCT:
                return DatabaseColumnType.STRING;
            case java.sql.Types.ARRAY:
                return DatabaseColumnType.STRING;
            case java.sql.Types.BLOB:
                return DatabaseColumnType.STRING;
            case java.sql.Types.CLOB:
                return DatabaseColumnType.STRING;
            case java.sql.Types.REF:
                return DatabaseColumnType.STRING;
            case java.sql.Types.BOOLEAN:
                return DatabaseColumnType.BOOLEAN;
            case java.sql.Types.INTEGER:
                return DatabaseColumnType.NUMBER;

            default:
                return DatabaseColumnType.STRING;
        }

    }

}
