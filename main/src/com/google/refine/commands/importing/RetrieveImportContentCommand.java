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
import com.google.refine.commands.importing.ImportJob.State;
import com.google.refine.model.meta.ImportSource;
import com.google.refine.util.ParsingUtilities;

public class RetrieveImportContentCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("retrieve-import-content_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * The uploaded file is in the POST body as a "file part". If
         * we call request.getParameter() then the POST body will get
         * read and we won't have a chance to parse the body ourselves.
         * This is why we have to parse the URL for parameters ourselves.
         * Don't call request.getParameter() before calling internalImport().
         */
        Properties options = ParsingUtilities.parseUrlParameters(request);
        
        long jobID = Long.parseLong(options.getProperty("jobID"));
        ImportJob job = ImportManager.singleton().getJob(jobID);
        if (job == null) {
            respondWithErrorPage(request, response, "No such import job", null);
            return;
        } else if (job.state != State.NEW) {
            respondWithErrorPage(request, response, "Import job already started", null);
            return;
        }
        
        Class<? extends ImportSource> importSourceClass =
            ImportManager.getImportSourceClass(options.getProperty("source"));
        if (importSourceClass == null) {
            respondWithErrorPage(request, response, "No such import source class", null);
            return;
        }

        try {
            ImportSource importSource = importSourceClass.newInstance();
            job.importSource = importSource;
            job.state = State.RETRIEVING_DATA;
            
            importSource.retrieveContent(request, options, job);
            
            job.retrievingProgress = 100;
            job.state = State.READY;
        } catch (Throwable e) {e.printStackTrace();
            job.state = State.ERROR;
            job.errorMessage = e.getLocalizedMessage();
            job.exception = e;
            
            respondWithErrorPage(request, response, "Failed to kick start import job", e);
        }
    }
}
