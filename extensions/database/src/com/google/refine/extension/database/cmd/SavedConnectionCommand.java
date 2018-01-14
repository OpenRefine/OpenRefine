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
package com.google.refine.extension.database.cmd;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseUtils;


public class SavedConnectionCommand extends DatabaseCommand {
 
    private static final Logger logger = LoggerFactory.getLogger("SavedConnectionCommand");
    

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        if(logger.isDebugEnabled()) {
            logger.debug("SavedConnectionCommand::Get::connectionName::{}", request.getParameter("connectionName"));
        }
        
        String connectionName = request.getParameter("connectionName");
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            if(connectionName == null || connectionName.isEmpty()) {
                writeSavedConnectionResponse(response);
            }else {
           
                DatabaseConfiguration savedConnection = DatabaseUtils.getSavedConnection(connectionName);
                writeSavedConnectionResponse(response, savedConnection);
                
            }
            
        } catch (Exception e) {
            logger.error("Exception while loading settings {}", e);
        }
    }

    
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        
        if(logger.isDebugEnabled()) {
            logger.debug("SavedConnectionCommand::Delete Connection: {}", request.getParameter("connectionName"));
        }

        String connectionName  = request.getParameter("connectionName");
        
        DatabaseConfiguration savedConn = DatabaseUtils.getSavedConnection(connectionName);
        if(savedConn == null) {
            //logger.error("Connection With name:: {} does not exist!", request.getParameter("connectionName"));
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Connection with name " + connectionName + " does not exists!");
            response.flushBuffer();
            return;
        }

        try {
            
            DatabaseUtils.deleteSavedConnections(connectionName);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            writeSavedConnectionResponse(response);
          
        } catch (Exception e) {
            logger.error("Exception while Deleting Connection with name: {}, error:{}",connectionName, e);
        }
    }

    /**
     * 
     * @param response
     * @param savedConnection
     * @throws IOException
     * @throws JSONException
     */
    private void writeSavedConnectionResponse(HttpServletResponse response, DatabaseConfiguration savedConnection) throws IOException, JSONException {
        Writer w = response.getWriter();
        try {
            JSONWriter writer = new JSONWriter(w);
            
            writer.object();
            writer.key(DatabaseUtils.SAVED_CONNECTION_KEY);
            writer.array();
            
            writer.object();
            writer.key("connectionName");
            writer.value(savedConnection.getConnectionName());
            
            writer.key("databaseType");
            writer.value(savedConnection.getDatabaseType());

            writer.key("databaseHost");
            writer.value(savedConnection.getDatabaseHost());

            writer.key("databasePort");
            writer.value(savedConnection.getDatabasePort());

            writer.key("databaseName");
            writer.value(savedConnection.getDatabaseName());

            writer.key("databasePassword");
           
            //
            String dbPasswd = savedConnection.getDatabasePassword();
            if(dbPasswd != null && !dbPasswd.isEmpty()) {
                dbPasswd = DatabaseUtils.decrypt(savedConnection.getDatabasePassword());
                //logger.info("Decrypted Password::" + dbPasswd);
            }
            writer.value(dbPasswd);
            //
            
           // writer.value(savedConnection.getDatabasePassword());

            writer.key("databaseSchema");
            writer.value(savedConnection.getDatabaseSchema());
            
            writer.key("databaseUser");
            writer.value(savedConnection.getDatabaseUser());

            writer.endObject();
            writer.endArray();
            
            writer.endObject();
            
        }finally {
            w.flush();
            w.close();
        }
        
    }
    /**
     * 
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    private void writeSavedConnectionResponse(HttpServletResponse response) throws IOException, JSONException {
        Writer w = response.getWriter();
        try {
            
            List<DatabaseConfiguration> savedConnections = DatabaseUtils.getSavedConnections();
            JSONWriter writer = new JSONWriter(w);

            writer.object();
            writer.key(DatabaseUtils.SAVED_CONNECTION_KEY);
            writer.array();

            int size = savedConnections.size();

            for (int i = 0; i < size; i++) {
                
                writer.object();
                DatabaseConfiguration dbConfig = (DatabaseConfiguration) savedConnections.get(i);

                writer.key("connectionName");
                writer.value(dbConfig.getConnectionName());

                writer.key("databaseType");
                writer.value(dbConfig.getDatabaseType());

                writer.key("databaseHost");
                writer.value(dbConfig.getDatabaseHost());

                writer.key("databasePort");
                writer.value(dbConfig.getDatabasePort());

                writer.key("databaseName");
                writer.value(dbConfig.getDatabaseName());

                writer.key("databasePassword");
                
                String dbPasswd = dbConfig.getDatabasePassword();
                if(dbPasswd != null && !dbPasswd.isEmpty()) {
                    dbPasswd = DatabaseUtils.decrypt(dbConfig.getDatabasePassword());
                }
               // writer.value(dbConfig.getDatabasePassword());
                writer.value(dbPasswd);
                
                writer.key("databaseSchema");
                writer.value(dbConfig.getDatabaseSchema());
                
                writer.key("databaseUser");
                writer.value(dbConfig.getDatabaseUser());

                writer.endObject();

            }
            writer.endArray();
            writer.endObject();
           // logger.info("Saved Connection Get Response sent");
        } finally {
            w.flush();
            w.close();
        }
    }

    /**
     * Add a new Saved JDBC connection configuration
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
       
        if(logger.isDebugEnabled()) {
            logger.debug("doPost Connection: {}", request.getParameter("connectionName"));
        }
        
        DatabaseConfiguration jdbcConfig = getJdbcConfiguration(request);
        

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
        if(jdbcConfig.getConnectionName() == null) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Connection Name is Required!");
            response.flushBuffer();
            return;
        }
     
        DatabaseConfiguration savedConn = DatabaseUtils.getSavedConnection(jdbcConfig.getConnectionName());
        if(savedConn != null) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Connection with name " + jdbcConfig.getConnectionName() + " already exists!");
            response.flushBuffer();
            return;
        }
        
      
        if(jdbcConfig.getDatabasePassword() != null) {
            //logger.debug("SavedConnectionCommand::Post::password::{}", jdbcConfig.getDatabasePassword());
           jdbcConfig.setDatabasePassword(DatabaseUtils.encrypt(jdbcConfig.getDatabasePassword()));
        }
        
        DatabaseUtils.addToSavedConnections(jdbcConfig);

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            writeSavedConnectionResponse(response); 
        } catch (Exception e) {
            logger.error("Exception while loading settings {}", e);
        }
   
    }



    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
     
        
        if(logger.isDebugEnabled()) {
            logger.debug("databaseType::{} " , request.getParameter("databaseHost"));
        }
       // logger.info("databaseHost::{} " , request.getParameter("databaseServer"));
        
        DatabaseConfiguration jdbcConfig = getJdbcConfiguration(request);
        StringBuilder sb = new StringBuilder();
        boolean error = false;
        if(jdbcConfig.getConnectionName() == null) {
            sb.append("Connection Name, ");
            error = true;
        }
        if(jdbcConfig.getDatabaseHost() == null) {
            sb.append("Database Host, ");
            error = true;
        }
        if(jdbcConfig.getDatabaseUser() == null) {
            sb.append("Database User, ");
            error = true;
        }
        if(jdbcConfig.getDatabaseName() == null) {
            sb.append("Database Name, ");
            error = true;
        }
        if(error) {
            sb.append(" is missing");
            logger.debug("Connection Parameter errors::{}", sb.toString());
            response.sendError(HttpStatus.SC_BAD_REQUEST, sb.toString());
        }
       
       if(logger.isDebugEnabled()) {
         logger.debug("Connection Config:: {}", jdbcConfig.getConnectionName());
       }
        
        if(jdbcConfig.getDatabasePassword() != null) {
            jdbcConfig.setDatabasePassword(DatabaseUtils.encrypt(jdbcConfig.getDatabasePassword()));
         }
        
        DatabaseUtils.editSavedConnections(jdbcConfig);
       
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            writeSavedConnectionResponse(response);
          
        } catch (Exception e) {
            logger.error("Exception while loading settings {}", e);
        }
    }

    
}
