/*

Copyright 2011, Google Inc.
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

package com.google.refine.commands.importing;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.ImportingController;
import com.google.refine.importing.ImportingManager;
import com.google.refine.util.ParsingUtilities;

public class ImportingControllerCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("importing-controller_command");
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	if(!hasValidCSRFTokenAsGET(request)) {
    		respondCSRFError(response);
    		return;
    	}

        ImportingController controller = getController(request);
        if (controller != null) {
        	response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            try {
                controller.doPost(request, response);
            } catch (IOException e) {
                HttpUtilities.respond(response, "error", e.getMessage());
            }
        } else {
            HttpUtilities.respond(response, "error", "No such import controller");
        }
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ImportingController controller = getController(request);
        if (controller != null) {
        	response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            controller.doPost(request, response);
        } else {
            HttpUtilities.respond(response, "error", "No such import controller");
        }
    }
    
    private ImportingController getController(HttpServletRequest request) {
        /*
         * The uploaded file is in the POST body as a "file part". If
         * we call request.getParameter() then the POST body will get
         * read and we won't have a chance to parse the body ourselves.
         * This is why we have to parse the URL for parameters ourselves.
         * Don't call request.getParameter() before calling internalImport().
         */
        Properties options = ParsingUtilities.parseUrlParameters(request);
        String name = options.getProperty("controller");
        if (name != null) {
            return ImportingManager.controllers.get(name);
        }
        return null;
    }
}
