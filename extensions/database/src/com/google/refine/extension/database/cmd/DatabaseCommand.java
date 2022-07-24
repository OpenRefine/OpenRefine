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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseServiceException;

public abstract class DatabaseCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger("DatabaseCommand");

    /**
     * 
     * @param request
     * @return
     */
    protected DatabaseConfiguration getJdbcConfiguration(HttpServletRequest request) {
        DatabaseConfiguration jdbcConfig = new DatabaseConfiguration();

        jdbcConfig.setConnectionName(request.getParameter("connectionName"));
        jdbcConfig.setDatabaseType(request.getParameter("databaseType"));
        jdbcConfig.setDatabaseHost(request.getParameter("databaseServer"));

        String dbPort = request.getParameter("databasePort");
        if (dbPort != null) {
            try {
                jdbcConfig.setDatabasePort(Integer.parseInt(dbPort));
            } catch (NumberFormatException nfe) {
            }
        }

        jdbcConfig.setDatabaseUser(request.getParameter("databaseUser"));
        jdbcConfig.setDatabasePassword(request.getParameter("databasePassword"));
        jdbcConfig.setDatabaseName(request.getParameter("initialDatabase"));
        jdbcConfig.setDatabaseSchema(request.getParameter("initialSchema"));

        if (logger.isDebugEnabled()) {
            logger.debug("JDBC Configuration: {}", jdbcConfig);
        }
        return jdbcConfig;
    }

    /**
     * 
     * @param status
     * @param response
     * @param e
     * @throws IOException
     */
    protected void sendError(int status, HttpServletResponse response, Exception e)
            throws IOException {

        // logger.info("sendError::{}", writer);
        response.sendError(status, e.getMessage());

    }

    /**
     * 
     * @param status
     * @param response
     * @param e
     * @throws IOException
     */
    protected void sendError(int status, HttpServletResponse response, DatabaseServiceException e)
            throws IOException {

        String message = "";

        if (e.getSqlState() != null) {

            message = message + "SqlCode:" + e.getSqlCode() + "SqlState" + e.getSqlState();
        }

        message = message + e.getMessage();

        response.sendError(status, e.getMessage());

    }

}
