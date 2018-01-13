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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.google.refine.ProjectManager;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.model.DatabaseInfo;


public class TestQueryCommand extends DatabaseCommand {

    private static final Logger logger = LoggerFactory.getLogger("TestQueryCommand");
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
       
        DatabaseConfiguration dbConfig = getJdbcConfiguration(request);
        String query = request.getParameter("query");
        
        if(logger.isDebugEnabled()) {
            logger.debug("TestQueryCommand::Post::DatabaseConfiguration::{}::Query::{} " ,dbConfig, query);
        }
      
        
        //ProjectManager.singleton.setBusy(true);
        try {
           
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            
            try {
                DatabaseInfo databaseInfo = DatabaseService.get(dbConfig.getDatabaseType())
                        .testQuery(dbConfig, query);
                ObjectMapper mapperObj = new ObjectMapper();
               
                response.setStatus(HttpStatus.SC_OK);
                String jsonStr = mapperObj.writeValueAsString(databaseInfo);
                if(logger.isDebugEnabled()) {
                    logger.debug("TestQueryCommand::Post::Result::{} " ,jsonStr);
                }
                
                writer.object();
                writer.key("code"); 
                writer.value("ok");
                writer.key("QueryResult"); 
                writer.value(jsonStr);
                writer.endObject();
               
               
            } catch (DatabaseServiceException e) {
                logger.error("TestQueryCommand::Post::DatabaseServiceException::{}", e);
                sendError(HttpStatus.SC_BAD_REQUEST, response, writer, e);

            } catch (Exception e) {
                logger.error("TestQueryCommand::Post::Exception::{}", e);
                sendError(HttpStatus.SC_BAD_REQUEST,response, writer, e);
            } finally {
                w.close();
            }
        } catch (Exception e) {
            logger.error("TestQueryCommand::Post::Exception::{}", e);
            throw new ServletException(e);
        }
//        finally {
//           // ProjectManager.singleton.setBusy(false);
//        }

        
    }

}
