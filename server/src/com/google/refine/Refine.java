package com.google.refine;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.apache.log4j.Level;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeberry.jdatapath.DataPath;
import com.codeberry.jdatapath.JDataPathSystem;
import com.google.util.threads.ThreadPoolExecutorAdapter;

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
        System.setProperty("org.mortbay.log.class","org.mortbay.log.Slf4jLog");
        
        // tell macosx to keep the menu associated with the screen and what the app title is
        System.setProperty("apple.laf.useScreenMenuBar", "true");  
        System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode", "false"); 
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Google Refine");
        
        // tell the signpost library to log
        //System.setProperty("debug","true");
            
        // if not already set, make sure jython can find its python stuff
        if (System.getProperty("python.path") == null) {
            System.setProperty("python.path","lib/python");
        }
        
        // set the log verbosity level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.toLevel(Configurations.get("refine.verbosity","info")));

        port = Configurations.getInteger("refine.port",DEFAULT_PORT);
        host = Configurations.get("refine.host",DEFAULT_HOST);

        Refine refine = new Refine();
        
        refine.init(args);
    }

    public void init(String[] args) throws Exception {

        RefineServer server = new RefineServer();
        server.init(host,port);

        boolean headless = Configurations.getBoolean("refine.headless",false);
        if (!headless) {
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
    
    public void init(String host, int port) throws Exception {
        logger.info("Starting Server bound to '" + host + ":" + port + "'");

        String memory = Configurations.get("refine.memory");
        if (memory != null) logger.info("Max memory size: " + memory);
        
        int maxThreads = Configurations.getInteger("refine.queue.size", 30);
        int maxQueue = Configurations.getInteger("refine.queue.max_size", 300);
        long keepAliveTime = Configurations.getInteger("refine.queue.idle_time", 60);

        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(maxQueue);
        
        threadPool = new ThreadPoolExecutor(maxThreads, maxQueue, keepAliveTime, TimeUnit.SECONDS, queue);

        this.setThreadPool(new ThreadPoolExecutorAdapter(threadPool));
        
        Connector connector = new SocketConnector();
        connector.setPort(port);
        connector.setHost(host);
        connector.setMaxIdleTime(Configurations.getInteger("refine.connection.max_idle_time",60000));
        connector.setStatsOn(false);
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
        
        logger.info("Initializing context: '" + contextPath + "' from '" + webapp.getAbsolutePath() + "'");
        WebAppContext context = new WebAppContext(webapp.getAbsolutePath(), contextPath);
        context.setMaxFormContentSize(1048576);

        this.setHandler(context);
        this.setStopAtShutdown(true);
        this.setSendServerVersion(true);

        // Enable context autoreloading
        if (Configurations.getBoolean("refine.autoreload",false)) {
            scanForUpdates(webapp, context);
        }
        
        // start the server
        this.start();
        
        configure(context);
    }
    
    @Override
    protected void doStop() throws Exception {    
        try {
            // shutdown our scheduled tasks first, if any
            if (threadPool != null) threadPool.shutdown();
            
            // then let the parent stop
            super.doStop();
        } catch (InterruptedException e) {
            // ignore
        }
    }
        
    static private boolean isWebapp(File dir) {
        if (dir == null) return false;
        if (!dir.exists() || !dir.canRead()) return false;
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

        scanner.start();
    }
    
    static private void findFiles(final String extension, File baseDir, final Collection<File> found) {
        baseDir.listFiles(new FileFilter() {
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
        
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            try {
                // NOTE(SM): finding the "local data app" in windows from java is actually a PITA
                // see http://stackoverflow.com/questions/1198911/how-to-get-local-application-data-folder-in-java
                // so we're using a library that uses JNI to ask directly the win32 APIs, 
                // it's not elegant but it's the safest bet.
                
                DataPath localDataPath = JDataPathSystem.getLocalSystem().getLocalDataPath("Google");
                
                dataDir = new File(new File(fixWindowsUnicodePath(localDataPath.getPath())), "Refine");
                
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
                
                dataDir = new File(new File(parentDir, "Google"), "Refine");
            }
        } else if (os.contains("mac os x")) {
            // on macosx, use "~/Library/Application Support"
            String home = System.getProperty("user.home");
            String data_home = (home != null) ? home + "/Library/Application Support/Google/Refine" : ".google-refine"; 
            
            dataDir = new File(data_home);
            
        } else { // most likely a UNIX flavor
            // start with the XDG environment
            // see http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
            String data_home = System.getenv("XDG_DATA_HOME");
            if (data_home == null) { // if not found, default back to ~/.local/share
                String home = System.getProperty("user.home");
                if (home == null) home = ".";
                data_home = home + "/.local/share";
            }
            
            dataDir = new File(data_home + "/google-refine");
        }
        
        // If refine data dir doesn't exist, try to find and move gridworks data dir over
        if (!dataDir.exists()) {
            File gridworksDir;
            if (dataDir.getName().equals("Refine")) {
                gridworksDir = new File(dataDir.getParentFile().getParentFile(), "Gridworks");
            } else if (dataDir.getName().startsWith(".")) {
                gridworksDir = new File(dataDir.getParent(), ".gridworks");
            } else {
                gridworksDir = new File(dataDir.getParent(), ".gridworks");
            }
            
            if (gridworksDir.exists()) {
                gridworksDir.renameTo(dataDir);
            } else {
                dataDir.mkdirs();
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

    public static boolean MACOSX = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));
    
    private URI uri;
    
    public void init(String host, int port) throws Exception {

        uri = new URI("http://" + host + ":" + port + "/");

        if (MACOSX) {

            // for more info on the code found here that is macosx-specific see:
            //  http://developer.apple.com/mac/library/documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html
            //  http://developer.apple.com/mac/library/releasenotes/CrossPlatform/JavaSnowLeopardUpdate1LeopardUpdate6RN/NewandNoteworthy/NewandNoteworthy.html

            JMenuBar mb = new JMenuBar(); 
            JMenu m = new JMenu("Open");
            JMenuItem mi = new JMenuItem("Open New Refine Window...");
            mi.addActionListener(this);
            m.add(mi);
            mb.add(m);

            Class<?> applicationClass = Class.forName("com.apple.eawt.Application"); 
            Object macOSXApplication = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
            Method setDefaultMenuBar = applicationClass.getDeclaredMethod("setDefaultMenuBar", new Class[] { JMenuBar.class });
            setDefaultMenuBar.invoke(macOSXApplication, new Object[] { mb });
           
            // FIXME(SM): this part below doesn't seem to work, I get a NPE but I have *no* idea why, suggestions?
            
//            PopupMenu dockMenu = new PopupMenu("dock");
//            MenuItem mmi = new MenuItem("Open new Refine Window...");
//            mmi.addActionListener(this);
//            dockMenu.add(mmi);
//            this.add(dockMenu);
//
//            Method setDockMenu = applicationClass.getDeclaredMethod("setDockMenu", new Class[] { PopupMenu.class });
//            setDockMenu.invoke(macOSXApplication, new Object[] { dockMenu });
        }
        
        openBrowser();
    }
    
    public void actionPerformed(ActionEvent e) { 
      String item = e.getActionCommand(); 
      if (item.startsWith("Open")) {
          openBrowser();
      }
    } 
    
    private void openBrowser() {
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

    public void run() {

        // Tell the server we want to try and shutdown gracefully
        // this means that the server will stop accepting new connections
        // right away but it will continue to process the ones that
        // are in execution for the given timeout before attempting to stop
        // NOTE: this is *not* a blocking method, it just sets a parameter
        //       that _server.stop() will rely on
        _server.setGracefulShutdown(3000);

        try {
            _server.stop();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
    