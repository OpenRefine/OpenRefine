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

package org.openrefine.importing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.simile.butterfly.ButterflyModule;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.RefineServlet;

public class ImportingManager {

    final static Logger logger = LoggerFactory.getLogger("importing");

    static private RefineServlet servlet;
    static private File importDir;

    final static private Map<Long, ImportingJob> jobs = Collections.synchronizedMap(new HashMap<Long, ImportingJob>());
    static private long jobIdCounter = 0;
    final static private Object jobIdLock = new Object();

    // Mapping from controller name to controller
    final static public Map<String, ImportingController> controllers = new HashMap<String, ImportingController>();

    // timer for periodically deleting stale importing jobs
    static private ScheduledExecutorService service;

    final static private long TIMER_PERIOD = 10; // 10 minutes
    final static private long STALE_PERIOD = 60 * 60 * 1000; // 60 minutes in milliseconds

    static private class CleaningTimerTask implements Runnable {

        @Override
        public void run() {
            // An exception here will keep future runs of this task from happening,
            // but won't affect other timer tasks
            cleanUpStaleJobs();
        }
    }

    static public void initialize(RefineServlet servlet) {
        ImportingManager.servlet = servlet;

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(new CleaningTimerTask(), TIMER_PERIOD, TIMER_PERIOD, TimeUnit.MINUTES);
    }

    static public void registerController(ButterflyModule module, String name, ImportingController controller) {
        String key = module.getName() + "/" + name;
        controllers.put(key, controller);

        controller.init(servlet);
    }

    static synchronized public File getImportDir() {
        if (importDir == null) {
            File tempDir = servlet.getTempDir();
            importDir = tempDir == null ? new File(".import-temp") : new File(tempDir, "import");

            if (importDir.exists()) {
                try {
                    // start fresh
                    FileUtils.deleteDirectory(importDir);
                } catch (IOException e) {
                }
            }
            importDir.mkdirs();
        }
        return importDir;
    }

    static public ImportingJob createJob() {
        long id;

        synchronized (jobIdLock) {
            ++jobIdCounter;

            // Avoid negative job id's when the counter wraps around.
            if (jobIdCounter < 0) {
                jobIdCounter = 1;
            }

            id = jobIdCounter;
        }

        File jobDir = new File(getImportDir(), Long.toString(id));

        ImportingJob job = new ImportingJob(id, jobDir);
        jobs.put(id, job);

        return job;
    }

    static public ImportingJob getJob(long id) {
        return jobs.get(id);
    }

    static public void disposeJob(long id) {
        ImportingJob job = getJob(id);
        if (job != null) {
            job.dispose();
            jobs.remove(id);
        }
    }

    static public class ImportingConfiguration {

        @JsonProperty("formats")
        public Map<String, ImportingFormat> getFormats() {
            return FormatRegistry.getFormatToRecord();
        }

        @JsonProperty("mimeTypeToFormat")
        public Map<String, String> getMimeTypeToFormat() {
            return FormatRegistry.getMimeTypeToFormat();
        }

        @JsonProperty("extensionToFormat")
        public Map<String, String> getExtensionToFormat() {
            return FormatRegistry.getExtensionToFormat();
        }
    }

    static private void cleanUpStaleJobs() {
        long now = System.currentTimeMillis();
        Collection<Long> keys;
        synchronized (jobs) {
            keys = new ArrayList<Long>(jobs.keySet());
        }
        for (Long id : keys) {
            ImportingJob job = jobs.get(id);
            if (job != null && !job.updating && now - job.lastTouched > STALE_PERIOD) {
                job.dispose();
                jobs.remove(id);
                logger.info("Removed Stale Import Job ID " + id);
            }
        }
    }
}
