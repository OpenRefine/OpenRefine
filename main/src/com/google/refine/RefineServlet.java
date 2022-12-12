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

package com.google.refine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;

import edu.mit.simile.butterfly.Butterfly;
import edu.mit.simile.butterfly.ButterflyModule;

public class RefineServlet extends Butterfly {

    static private String ASSIGNED_VERSION = "3.8-SNAPSHOT";

    static public String VERSION = "";
    static public String REVISION = "";
    static public String FULL_VERSION = "";
    static public String FULLNAME = "OpenRefine ";

    static final long serialVersionUID = 2386057901503517403L;

    static private final String JAVAX_SERVLET_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
    private File tempDir = null;

    static private RefineServlet s_singleton;
    static private File s_dataDir;

    static final private Map<String, Command> commands = new HashMap<String, Command>();

    // timer for periodically saving projects
    static private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    static final Logger logger = LoggerFactory.getLogger("refine");

    static protected class AutoSaveTimerTask implements Runnable {

        @Override
        public void run() {
            try {
                ProjectManager.singleton.save(false); // quick, potentially incomplete save
            } catch (final Throwable e) {
                // Not the best, but we REALLY want this to keep trying
            }
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();

        VERSION = getInitParameter("refine.version");
        REVISION = getInitParameter("refine.revision");

        if (VERSION.equals("$VERSION")) {
            VERSION = ASSIGNED_VERSION;
        }
        if (REVISION.equals("$REVISION")) {
            ClassLoader classLoader = getClass().getClassLoader();
            try {
                InputStream gitStats = classLoader.getResourceAsStream("git.properties");
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode parsedGit = mapper.readValue(gitStats, ObjectNode.class);
                REVISION = parsedGit.get("git.commit.id.abbrev").asText("TRUNK");
            } catch (IOException e) {
                REVISION = "TRUNK";
            }
        }

        FULL_VERSION = VERSION + " [" + REVISION + "]";
        FULLNAME += FULL_VERSION;

        logger.info("Starting " + FULLNAME + "...");

        s_singleton = this;

        logger.trace("> initialize");

        String data = getInitParameter("refine.data");

        if (data == null) {
            throw new ServletException("can't find servlet init config 'refine.data', I have to give up initializing");
        }
        logger.error("initializing FileProjectManager with dir");
        logger.error(data);
        s_dataDir = new File(data);
        FileProjectManager.initialize(s_dataDir);
        ImportingManager.initialize(this);

        long AUTOSAVE_PERIOD = Long.parseLong(getInitParameter("refine.autosave"));

        service.scheduleWithFixedDelay(new AutoSaveTimerTask(), AUTOSAVE_PERIOD,
                AUTOSAVE_PERIOD, TimeUnit.MINUTES);

        logger.trace("< initialize");
    }

    @Override
    public void destroy() {
        logger.trace("> destroy");

        // cancel automatic periodic saving and force a complete save.
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
        if (ProjectManager.singleton != null) {
            ProjectManager.singleton.dispose();
            ProjectManager.singleton = null;
        }

        logger.trace("< destroy");

        super.destroy();
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo().startsWith("/command/")) {
            String commandKey = getCommandKey(request);
            Command command = commands.get(commandKey);
            if (command != null) {
                if (request.getMethod().equals("GET")) {
                    if (!logger.isTraceEnabled() && command.logRequests()) {
                        logger.info("GET {}", request.getPathInfo());
                    }
                    logger.trace("> GET {}", commandKey);
                    command.doGet(request, response);
                    logger.trace("< GET {}", commandKey);
                } else if (request.getMethod().equals("POST")) {
                    if (!logger.isTraceEnabled() && command.logRequests()) {
                        logger.info("POST {}", request.getPathInfo());
                    }
                    logger.trace("> POST {}", commandKey);
                    command.doPost(request, response);
                    logger.trace("< POST {}", commandKey);
                } else if (request.getMethod().equals("PUT")) {
                    if (!logger.isTraceEnabled() && command.logRequests()) {
                        logger.info("PUT {}", request.getPathInfo());
                    }
                    logger.trace("> PUT {}", commandKey);
                    command.doPut(request, response);
                    logger.trace("< PUT {}", commandKey);
                } else if (request.getMethod().equals("DELETE")) {
                    if (!logger.isTraceEnabled() && command.logRequests()) {
                        logger.info("DELETE {}", request.getPathInfo());
                    }
                    logger.trace("> DELETE {}", commandKey);
                    command.doDelete(request, response);
                    logger.trace("< DELETE {}", commandKey);
                } else {
                    response.sendError(HttpStatus.SC_METHOD_NOT_ALLOWED);
                }
            } else {
                response.sendError(HttpStatus.SC_NOT_FOUND);
            }
        } else {
            super.service(request, response);
        }
    }

    public ButterflyModule getModule(String name) {
        return _modulesByName.get(name);
    }

    protected String getCommandKey(HttpServletRequest request) {
        // A command path has this format: /command/module-name/command-name/...

        String path = request.getPathInfo().substring("/command/".length());

        int slash1 = path.indexOf('/');
        if (slash1 >= 0) {
            int slash2 = path.indexOf('/', slash1 + 1);
            if (slash2 > 0) {
                path = path.substring(0, slash2);
            }
        }

        return path;
    }

    public File getTempDir() {
        if (tempDir == null) {
            tempDir = (File) _config.getServletContext().getAttribute(JAVAX_SERVLET_CONTEXT_TEMPDIR);
            if (tempDir == null) {
                throw new RuntimeException("This app server doesn't support temp directories");
            }
        }
        return tempDir;
    }

    public File getTempFile(String name) {
        return new File(getTempDir(), name);
    }

    public File getCacheDir(String name) {
        File dir = new File(new File(s_dataDir, "cache"), name);
        dir.mkdirs();

        return dir;
    }

    public String getConfiguration(String name, String def) {
        return null;
    }

    /**
     * Register a single command.
     *
     * @param module
     *            the module the command belongs to
     * @param name
     *            command verb for command
     * @param commandObject
     *            object implementing the command
     * @return true if command was loaded and registered successfully
     */
    protected boolean registerOneCommand(ButterflyModule module, String name, Command commandObject) {
        return registerOneCommand(module.getName() + "/" + name, commandObject);
    }

    /**
     * Register a single command.
     *
     * @param path
     *            path for command
     * @param commandObject
     *            object implementing the command
     * @return true if command was loaded and registered successfully
     */
    protected boolean registerOneCommand(String path, Command commandObject) {
        if (commands.containsKey(path)) {
            return false;
        }

        commandObject.init(this);
        commands.put(path, commandObject);

        return true;
    }

    // Currently only for test purposes
    protected boolean unregisterCommand(String verb) {
        return commands.remove(verb) != null;
    }

    /**
     * Register a single command. Used by extensions.
     *
     * @param module
     *            the module the command belongs to
     * @param commandName
     *            command verb for command
     * @param commandObject
     *            object implementing the command
     * 
     * @return true if command was loaded and registered successfully
     */
    static public boolean registerCommand(ButterflyModule module, String commandName, Command commandObject) {
        return s_singleton.registerOneCommand(module, commandName, commandObject);
    }

    static private class ClassMapping {

        final String from;
        final String to;

        ClassMapping(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    static final private List<ClassMapping> classMappings = new ArrayList<ClassMapping>();

    /**
     * Add a mapping that determines how old class names can be updated to newer class names. Such updates are desirable
     * as the Java code changes from version to version. If the "from" argument ends with *, then it's considered a
     * prefix; otherwise, it's an exact string match.
     * 
     * @param from
     * @param to
     */
    static public void registerClassMapping(String from, String to) {
        classMappings.add(new ClassMapping(from, to.endsWith("*") ? to.substring(0, to.length() - 1) : to));
    }

    static {
        registerClassMapping("com.metaweb.*", "com.google.*");
        registerClassMapping("com.google.gridworks.*", "com.google.refine.*");
    }

    static final private Map<String, String> classMappingsCache = new HashMap<String, String>();
    static final private Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();

    // TODO(dfhuynh): Temporary solution until we figure out why cross butterfly module class resolution
    // doesn't entirely work
    static public void cacheClass(Class<?> klass) {
        classCache.put(klass.getName(), klass);
    }

    static public Class<?> getClass(String className) throws ClassNotFoundException {
        String toClassName = classMappingsCache.get(className);
        if (toClassName == null) {
            toClassName = className;

            for (ClassMapping m : classMappings) {
                if (m.from.endsWith("*")) {
                    if (toClassName.startsWith(m.from.substring(0, m.from.length() - 1))) {
                        toClassName = m.to + toClassName.substring(m.from.length() - 1);
                    }
                } else {
                    if (m.from.equals(toClassName)) {
                        toClassName = m.to;
                    }
                }
            }

            classMappingsCache.put(className, toClassName);
        }

        Class<?> klass = classCache.get(toClassName);
        if (klass == null) {
            klass = Class.forName(toClassName);
            classCache.put(toClassName, klass);
        }
        return klass;
    }

    /**
     * @deprecated extensions relying on HttpURLConnection should rather migrate to a more high-level and mature HTTP
     *             client. Use {@link RefineServlet#getUserAgent()} instead.
     */
    @Deprecated
    static public void setUserAgent(URLConnection urlConnection) {
        if (urlConnection instanceof HttpURLConnection) {
            setUserAgent((HttpURLConnection) urlConnection);
        }
    }

    /**
     * @deprecated extensions relying on HttpURLConnection should rather migrate to a more high-level and mature HTTP
     *             client. Use {@link RefineServlet#getUserAgent()} instead.
     */
    @Deprecated
    static public void setUserAgent(HttpURLConnection httpConnection) {
        httpConnection.addRequestProperty("User-Agent", getUserAgent());
    }

    static public String getUserAgent() {
        return "OpenRefine/" + FULL_VERSION;
    }
}
