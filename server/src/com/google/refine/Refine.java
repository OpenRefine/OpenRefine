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

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Level;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.util.Scanner;
import org.eclipse.jetty.util.thread.ThreadPool;
import com.google.util.threads.ThreadPoolExecutorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.Configurations;

/**
 * Main class for Refine server application. Starts an instance of the Jetty HTTP server / servlet container (inner
 * class Refine Server).
 */
public class Refine {

    static private final String DEFAULT_IFACE = "127.0.0.1";
    static private final int DEFAULT_PORT = 3333;

    static private int port;
    static private String host;
    static private String iface;

    final static Logger logger = LoggerFactory.getLogger("refine");

    public static void main(String[] args) throws Exception {

        // tell jetty to use SLF4J for logging instead of its own stuff
        System.setProperty("VERBOSE", "false");
        System.setProperty("org.eclipse.jetty.log.class", "org.eclipse.jetty.util.log.Slf4jLog");

        // tell macosx to keep the menu associated with the screen and what the app title is
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode", "false");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "OpenRefine");

        // tell the signpost library to log
        // System.setProperty("debug","true");

        // set the log verbosity level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.toLevel(Configurations.get("refine.verbosity", "info")));

        port = Configurations.getInteger("refine.port", DEFAULT_PORT);
        iface = Configurations.get("refine.interface", DEFAULT_IFACE);
        host = Configurations.get("refine.host", iface);
        if ("0.0.0.0".equals(host)) {
            host = "*";
        }

        System.setProperty("refine.display.new.version.notice", Configurations.get("refine.display.new.version.notice", "true"));
        Refine refine = new Refine();

        refine.init(args);
    }

    public void init(String[] args) throws Exception {

        RefineServer server = new RefineServer();
        server.init(iface, port, host);

        boolean headless = Configurations.getBoolean("refine.headless", false);
        if (headless) {
            System.setProperty("java.awt.headless", "true");
            logger.info("Running in headless mode");
        } else {
            try {
                RefineClient client = new RefineClient();
                if ("*".equals(host)) {
                    if ("0.0.0.0".equals(iface)) {
                        logger.warn("No refine.host specified while binding to interface 0.0.0.0, guessing localhost.");
                        client.init("localhost", port);
                    } else {
                        client.init(iface, port);
                    }
                } else {
                    client.init(host, port);
                }
            } catch (Exception e) {
                logger.warn("Sorry, some error prevented us from launching the browser for you.\n\n Point your browser to http://" + host
                        + ":" + port + "/ to start using Refine.");
            }
        }

        // hook up the signal handlers
        Runtime.getRuntime().addShutdownHook(
                new Thread(new ShutdownSignalHandler(server)));

        server.join();
    }
}

/* -------------- Refine Server ----------------- */

class RefineServer extends Server {

    final static Logger logger = LoggerFactory.getLogger("refine_server");

    public RefineServer() {
        super(createThreadPool());
    }

    private static ThreadPool createThreadPool() {
        int maxThreads = Configurations.getInteger("refine.queue.size", 30);
        int maxQueue = Configurations.getInteger("refine.queue.max_size", 300);
        long keepAliveTime = Configurations.getInteger("refine.queue.idle_time", 60);
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(maxQueue);
        return new ThreadPoolExecutorAdapter(new ThreadPoolExecutor(maxThreads, maxQueue, keepAliveTime, TimeUnit.SECONDS, queue));
    }

    private ThreadPoolExecutor threadPool;

    public void init(String iface, int port, String host) throws Exception {
        logger.info("Starting Server bound to '" + iface + ":" + port + "'");

        String memory = Configurations.get("refine.memory");
        if (memory != null) {
            logger.info("refine.memory size: " + memory + " JVM Max heap: " + Runtime.getRuntime().maxMemory());
        }

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        HttpConnectionFactory httpFactory = new HttpConnectionFactory(httpConfig);
        ServerConnector connector = new ServerConnector(this, httpFactory);
        connector.setPort(port);
        connector.setHost(iface);
        connector.setIdleTimeout(Configurations.getInteger("server.connection.max_idle_time", 60000));
        this.addConnector(connector);

        File webapp = new File(Configurations.get("refine.webapp", "main/webapp"));

        if (!isWebapp(webapp)) {
            webapp = new File("main/webapp");
            if (!isWebapp(webapp)) {
                webapp = new File("webapp");
                if (!isWebapp(webapp)) {
                    logger.warn("Warning: Failed to find web application at '" + webapp.getAbsolutePath() + "'");
                    System.exit(-1);
                }
            }
        }

        final String contextPath = Configurations.get("refine.context_path", "/");
        final int maxFormContentSize = Configurations.getInteger("refine.max_form_content_size", 64 * 1048576); // 64MB

        logger.info("Initializing context: '" + contextPath + "' from '" + webapp.getAbsolutePath() + "'");
        WebAppContext context = new WebAppContext(webapp.getAbsolutePath(), contextPath);
        context.setMaxFormContentSize(maxFormContentSize);

        if ("*".equals(host)) {
            this.setHandler(context);
        } else {
            ValidateHostHandler wrapper = new ValidateHostHandler(host);
            wrapper.setHandler(context);
            this.setHandler(wrapper);
        }

        this.setStopAtShutdown(true);
        StatisticsHandler handler = new StatisticsHandler();
        handler.setServer(this);
        handler.setHandler(this.getHandler());
        this.addBean(handler);
        // Tell the server we want to try and shutdown gracefully
        // this means that the server will stop accepting new connections
        // right away but it will continue to process the ones that
        // are in execution for the given timeout before attempting to stop
        // NOTE: this is *not* a blocking method, it just sets a parameter
        // that _server.stop() will rely on
        this.setStopTimeout(30000);

        // Enable context autoreloading
        if (Configurations.getBoolean("refine.autoreload", false)) {
            scanForUpdates(webapp, context);
        }

        // start the server
        try {
            this.start();
        } catch (BindException e) {
            logger.error("Failed to start server - is there another copy running already on this port/address?");
            throw e;
        }

        configure(context);
    }

    @Override
    protected void doStop() throws Exception {
        try {
            // shutdown our scheduled tasks first, if any
            if (threadPool != null) {
                threadPool.shutdown();
            }
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // stop current thread
            Thread.currentThread().interrupt();
        }
        // then let the parent stop
        super.doStop();
    }

    static private boolean isWebapp(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists() || !dir.canRead()) {
            return false;
        }
        File webXml = new File(dir, "WEB-INF/web.xml");
        return webXml.exists() && webXml.canRead();
    }

    static private void scanForUpdates(final File contextRoot, final WebAppContext context) {
        List<File> scanList = new ArrayList<File>();

        scanList.add(new File(contextRoot, "WEB-INF/web.xml"));
        findFiles(".class", new File(contextRoot, "WEB-INF/classes"), scanList);
        findFiles(".jar", new File(contextRoot, "WEB-INF/lib"), scanList);

        logger.info("Starting autoreloading scanner... ");

        Scanner scanner = new Scanner();
        scanner.setScanInterval(Configurations.getInteger("refine.scanner.period", 1));
        scanner.setScanDirs(scanList);
        scanner.setReportExistingFilesOnStartup(false);

        scanner.addListener(new Scanner.BulkListener() {

            @Override
            public void filesChanged(@SuppressWarnings("rawtypes") List changedFiles) {
                try {
                    logger.info("Stopping context: " + contextRoot.getAbsolutePath());
                    context.stop();

                    logger.info("Starting context: " + contextRoot.getAbsolutePath());
                    context.start();

                    configure(context);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        try {
            scanner.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static private void findFiles(final String extension, File baseDir, final Collection<File> found) {
        baseDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    findFiles(extension, pathname, found);
                } else if (pathname.getName().endsWith(extension)) {
                    found.add(pathname);
                }
                return false;
            }
        });
    }

    // inject configuration parameters in the servlets
    // NOTE: this is done *after* starting the server because jetty might override the init
    // parameters if we set them in the webapp context upon reading the web.xml file

    static private void configure(WebAppContext context) throws Exception {
        ServletHolder servlet = context.getServletHandler().getServlet("refine");
        if (servlet != null) {
            servlet.setInitParameter("refine.data", getDataDir());
            servlet.setInitParameter("butterfly.modules.path", getDataDir() + "/extensions");
            servlet.setInitParameter("refine.autosave", Configurations.get("refine.autosave", "5")); // default: 5
                                                                                                     // minutes
            servlet.setInitOrder(1);
            servlet.doStart();
        }

        servlet = context.getServletHandler().getServlet("refine-broker");
        if (servlet != null) {
            servlet.setInitParameter("refine.data", getDataDir() + "/broker");
            servlet.setInitParameter("refine.development", Configurations.get("refine.development", "false"));
            servlet.setInitOrder(1);
            servlet.doStart();
        }
    }

    static private String getDataDir() {
        String data_dir = Configurations.get("refine.data_dir");
        if (data_dir != null) {
            return data_dir;
        }

        File dataDir = null;
        File grefineDir = null;
        File gridworksDir = null;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            File parentDir = null;
            String appData = System.getenv("APPDATA");
            if (appData != null && appData.length() > 0) {
                // e.g., C:\Users\[userid]\AppData\Roaming
                parentDir = new File(appData);
            } else {
                // TODO migrate to System.getProperty("user.home")?
                String userProfile = System.getProperty("user.home");
                if (userProfile != null && userProfile.length() > 0) {
                    // e.g., C:\Users\[userid]
                    parentDir = new File(userProfile);
                }
            }

            if (parentDir == null) {
                parentDir = new File(".");
            }

            dataDir = new File(parentDir, "OpenRefine");
            grefineDir = new File(new File(parentDir, "Google"), "Refine");
            gridworksDir = new File(parentDir, "Gridworks");
        } else if (os.contains("os x")) {
            // on macosx, use "~/Library/Application Support"
            String home = System.getProperty("user.home");

            String data_home = (home != null) ? home + "/Library/Application Support/OpenRefine" : ".openrefine";
            dataDir = new File(data_home);

            String grefine_home = (home != null) ? home + "/Library/Application Support/Google/Refine" : ".google-refine";
            grefineDir = new File(grefine_home);

            String gridworks_home = (home != null) ? home + "/Library/Application Support/Gridworks" : ".gridworks";
            gridworksDir = new File(gridworks_home);
        } else { // most likely a UNIX flavor
            // start with the XDG environment
            // see http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
            String data_home = System.getenv("XDG_DATA_HOME");
            if (data_home == null) { // if not found, default back to ~/.local/share
                String home = System.getProperty("user.home");
                if (home == null) {
                    home = ".";
                }
                data_home = home + "/.local/share";
            }

            dataDir = new File(data_home + "/openrefine");
            grefineDir = new File(data_home + "/google/refine");
            gridworksDir = new File(data_home + "/gridworks");
        }

        // If refine data dir doesn't exist, try to find and move Google Refine or Gridworks data dir over
        if (!dataDir.exists()) {
            if (grefineDir.exists()) {
                if (gridworksDir.exists()) {
                    logger.warn("Found both Gridworks: " + gridworksDir
                            + " & Googld Refine dirs " + grefineDir);
                }
                if (grefineDir.renameTo(dataDir)) {
                    logger.info("Renamed Google Refine directory " + grefineDir
                            + " to " + dataDir);
                } else {
                    logger.error("FAILED to rename Google Refine directory "
                            + grefineDir
                            + " to " + dataDir);
                }
            } else if (gridworksDir.exists()) {
                if (gridworksDir.renameTo(dataDir)) {
                    logger.info("Renamed Gridworks directory " + gridworksDir
                            + " to " + dataDir);
                } else {
                    logger.error("FAILED to rename Gridworks directory "
                            + gridworksDir
                            + " to " + dataDir);
                }
            }
        }

        // Either rename failed or nothing to rename - create a new one
        if (!dataDir.exists()) {
            logger.info("Creating new workspace directory " + dataDir);
            if (!dataDir.mkdirs()) {
                logger.error("FAILED to create new workspace directory " + dataDir);
            }
        }

        return dataDir.getAbsolutePath();
    }

    /**
     * For Windows file paths that contain user IDs with non ASCII characters, those characters might get replaced with
     * ?. We need to use the environment APPDATA value to substitute back the original user ID.
     */
    static private String fixWindowsUnicodePath(String path) {
        int q = path.indexOf('?');
        if (q < 0) {
            return path;
        }
        int pathSep = path.indexOf(File.separatorChar, q);

        String goodPath = System.getenv("APPDATA");
        if (goodPath == null || goodPath.length() == 0) {
            goodPath = System.getenv("USERPROFILE");
            if (!goodPath.endsWith(File.separator)) {
                goodPath = goodPath + File.separator;
            }
        }

        int goodPathSep = goodPath.indexOf(File.separatorChar, q);

        return path.substring(0, q) + goodPath.substring(q, goodPathSep) + path.substring(pathSep);
    }

}

/* -------------- Refine Client ----------------- */

class RefineClient extends JFrame implements ActionListener {

    private static final long serialVersionUID = 7886547342175227132L;

    final static Logger logger = LoggerFactory.getLogger("refine-client");

    private URI uri;

    public void init(String host, int port) throws Exception {
        uri = new URI("http://" + host + ":" + port + "/");
        openBrowser();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String item = e.getActionCommand();
        if (item.startsWith("Open")) {
            openBrowser();
        }
    }

    private void openBrowser() {
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                openBrowserFallback();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void openBrowserFallback() throws IOException {
        Runtime rt = Runtime.getRuntime();

        if (SystemUtils.IS_OS_WINDOWS) {
            rt.exec("rundll32 url.dll,FileProtocolHandler " + uri);
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            rt.exec("open " + uri);
        } else if (SystemUtils.IS_OS_LINUX) {
            rt.exec("xdg-open " + uri);
        } else {
            logger.warn("Java Desktop class not supported on this platform. Please open %s in your browser", uri.toString());
        }
    }
}

class ShutdownSignalHandler implements Runnable {

    private Server _server;

    public ShutdownSignalHandler(Server server) {
        this._server = server;
    }

    @Override
    public void run() {

        try {
            _server.stop();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
