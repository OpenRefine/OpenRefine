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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.apache.log4j.Level;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Scanner;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeberry.jdatapath.DataPath;
import com.codeberry.jdatapath.JDataPathSystem;

/**
 * Main class for Refine server application.  Starts an instance of the
 * Jetty HTTP server / servlet container (inner class Refine Server).
 */
public class Refine {
    
    static private final String DEFAULT_HOST = "127.0.0.1";
    static private final int DEFAULT_PORT = 3333;
        
    static private int port;
    static private String host;

    final static Logger logger = LoggerFactory.getLogger("refine");
        
    public static void main(String[] args) throws Exception {
        
        // tell jetty to use SLF4J for logging instead of its own stuff
        System.setProperty("VERBOSE","false");
        
        System.setProperty("org.eclipse.jetty.util.log.class","org.eclipse.jetty.util.log.Slf4jLog");
        
        // tell macosx to keep the menu associated with the screen and what the app title is
        System.setProperty("apple.laf.useScreenMenuBar", "true");  
        System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode", "false"); 
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "OpenRefine");
        
        // tell the signpost library to log
        //System.setProperty("debug","true");
        
        // set the log verbosity level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.toLevel(Configurations.get("refine.verbosity","info")));

        port = Configurations.getInteger("refine.port",DEFAULT_PORT);
        host = Configurations.get("refine.host",DEFAULT_HOST);

        Refine refine = new Refine();
        
        refine.init(args);
    }

    public void init(String[] args) throws Exception {
        // in jetty 9, the thread pool is passed as a constructor argument:
//        RefineServer server = new RefineServer();
        int maxThreads = Configurations.getInteger("refine.queue.size", 30);
        int maxQueue = Configurations.getInteger("refine.queue.max_size", 300);
        long keepAliveTime = Configurations.getInteger("refine.queue.idle_time", 60);

        // use ExecutorThreadPool instead of QueuedThreadPool
        // http://stackoverflow.com/questions/5619735/jetty-configuration-threadpool
        ExecutorThreadPool threadPool = new ExecutorThreadPool(maxThreads, maxQueue, keepAliveTime, TimeUnit.SECONDS);
        RefineServer server = new RefineServer(threadPool);
        
        server.init(host,port);

        boolean headless = Configurations.getBoolean("refine.headless",false);
        if (headless) {
            System.setProperty("java.awt.headless", "true");
            logger.info("Running in headless mode");
        } else {
            try {
                RefineClient client = new RefineClient();
                client.init(host,port);
            } catch (Exception e) {
                logger.warn("Sorry, some error prevented us from launching the browser for you.\n\n Point your browser to http://" + host + ":" + port + "/ to start using Refine.");
            }
        }
        
        // hook up the signal handlers
        Runtime.getRuntime().addShutdownHook(
            new Thread(new ShutdownSignalHandler(server))
        );
 
        server.join();
    }
}

/* -------------- Refine Server ----------------- */

class RefineServer extends Server {
    
    final static Logger logger = LoggerFactory.getLogger("refine_server");
        
    private ThreadPoolExecutor threadPool;
    
    public RefineServer(ThreadPool threadPool) {
        super(threadPool);
    }

    public void init(String host, int port) throws Exception {
        logger.info("Starting Server bound to '" + host + ":" + port + "'");

        String memory = Configurations.get("refine.memory");
        if (memory != null) {
            logger.info("refine.memory size: " + memory + " JVM Max heap: " + Runtime.getRuntime().maxMemory());
        }
        
        // HTTP Configuration, hard code for now. Later can be read from configuration file when adding the embedding SSL feature
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(443);
        httpConfig.setOutputBufferSize(32768);

        // HTTP connector
        ServerConnector connector = new ServerConnector(this,new HttpConnectionFactory(httpConfig));        
        connector.setPort(port);
        connector.setHost(host);
        connector.setIdleTimeout(Configurations.getInteger("refine.connection.max_idle_time",60000));
        // Jetty 9 does not have the equivalence??
//        connector.setStatsOn(false);
        
        this.addConnector(connector);

        File webapp = new File(Configurations.get("refine.webapp","main/webapp"));

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

        final String contextPath = Configurations.get("refine.context_path","/");
        final int maxFormContentSize = Configurations.getInteger("refine.max_form_content_size", 1048576);
        
        logger.info("Initializing context: '" + contextPath + "' from '" + webapp.getAbsolutePath() + "'");
        WebAppContext context = new WebAppContext(webapp.getAbsolutePath(), contextPath);
        context.setMaxFormContentSize(maxFormContentSize);

        this.setHandler(context);
        this.setStopAtShutdown(true);
        
        // Enable context autoreloading
        if (Configurations.getBoolean("refine.autoreload",false)) {
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
            
            // then let the parent stop
            super.doStop();
        } catch (InterruptedException e) {
            // ignore
        }
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
        scanner.setScanInterval(Configurations.getInteger("refine.scanner.period",1));
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
            logger.error("Starting scanner failed: " + e.getMessage());
            throw new RuntimeException(e);
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
            servlet.setInitOrder(1);
            servlet.doStart();
            
            // TODO: need a decent way to do ServletHolder.initServlet() 
            servlet.getServlet();
        }

        servlet = context.getServletHandler().getServlet("refine-broker");
        if (servlet != null) {
            servlet.setInitParameter("refine.data", getDataDir() + "/broker");
            servlet.setInitParameter("refine.development", Configurations.get("refine.development","false"));
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
            try {
                // NOTE(SM): finding the "local data app" in windows from java is actually a PITA
                // see http://stackoverflow.com/questions/1198911/how-to-get-local-application-data-folder-in-java
                // so we're using a library that uses JNI to ask directly the win32 APIs, 
                // it's not elegant but it's the safest bet.
                
                dataDir = new File(fixWindowsUnicodePath(JDataPathSystem.getLocalSystem()
                        .getLocalDataPath("OpenRefine").getPath()));

                DataPath localDataPath = JDataPathSystem.getLocalSystem().getLocalDataPath("Google");

                // new: ./Google/Refine old: ./Gridworks
                grefineDir = new File(new File(fixWindowsUnicodePath(localDataPath.getPath())), "Refine");
                gridworksDir = new File(fixWindowsUnicodePath(JDataPathSystem.getLocalSystem()
                        .getLocalDataPath("Gridworks").getPath()));
            } catch (Error e) {
                /*
                 *  The above trick can fail, particularly on a 64-bit OS as the jdatapath.dll
                 *  we include is compiled for 32-bit. In this case, we just have to dig up
                 *  environment variables and try our best to find a user-specific path.
                 */
                
                logger.warn("Failed to use jdatapath to detect user data path: resorting to environment variables");
                
                File parentDir = null;
                String appData = System.getenv("APPDATA"); 
                if (appData != null && appData.length() > 0) {
                    // e.g., C:\Users\[userid]\AppData\Roaming
                    parentDir = new File(appData);
                } else {
                    String userProfile = System.getenv("USERPROFILE");
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
            }
        } else if (os.contains("mac os x")) {
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
                            + " & Googld Refine dirs " + grefineDir) ;
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
     * For Windows file paths that contain user IDs with non ASCII characters,
     * those characters might get replaced with ?. We need to use the environment
     * APPDATA value to substitute back the original user ID.
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
        if (!Desktop.isDesktopSupported()) {
            logger.warn("Java Desktop class not supported on this platform.  Please open %s in your browser",uri.toString());
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        // Tell the server we want to try and shutdown gracefully
        // this means that the server will stop accepting new connections
        // right away but it will continue to process the ones that
        // are in execution for the given timeout before attempting to stop
        // NOTE: this is *not* a blocking method, it just sets a parameter
        //       that _server.stop() will rely on
        
        _server.setStopTimeout(3000);

        try {
            _server.stop();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
    
