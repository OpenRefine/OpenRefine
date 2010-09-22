package com.google.refine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.io.FileProjectManager;

import edu.mit.simile.butterfly.Butterfly;
import edu.mit.simile.butterfly.ButterflyModule;

public class RefineServlet extends Butterfly {

    static private final String VERSION = "1.5";

    private static final long serialVersionUID = 2386057901503517403L;

    private static final String JAVAX_SERVLET_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";

    static private RefineServlet s_singleton;
    static private File             s_dataDir;
    
    static final private Map<String, Command> commands = new HashMap<String, Command>();

    // timer for periodically saving projects
    static private Timer _timer;

    final static Logger logger = LoggerFactory.getLogger("gridworks");

    public static String getVersion() {
        return VERSION;
    }

    final static protected long s_autoSavePeriod = 1000 * 60 * 5; // 5 minutes

    static protected class AutoSaveTimerTask extends TimerTask {
        public void run() {
            try {
                ProjectManager.singleton.save(false); // quick, potentially incomplete save
            } finally {
                _timer.schedule(new AutoSaveTimerTask(), s_autoSavePeriod);
                // we don't use scheduleAtFixedRate because that might result in
                // bunched up events when the computer is put in sleep mode
            }
        }
    }

    protected ServletConfig config;

    @Override
    public void init() throws ServletException {
        super.init();
        
        s_singleton = this;

        logger.trace("> initialize");

        String data = getInitParameter("gridworks.data");

        if (data == null) {
            throw new ServletException("can't find servlet init config 'gridworks.data', I have to give up initializing");
        }

        s_dataDir = new File(data);
        FileProjectManager.initialize(s_dataDir);

        if (_timer == null) {
            _timer = new Timer("autosave");
            _timer.schedule(new AutoSaveTimerTask(), s_autoSavePeriod);
        }

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

        this.config = null;

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
                    logger.trace("> GET {}", commandKey);
                    command.doGet(request, response);
                    logger.trace("< GET {}", commandKey);
                } else if (request.getMethod().equals("POST")) {
                    logger.trace("> POST {}", commandKey);
                    command.doPost(request, response);
                    logger.trace("< POST {}", commandKey);
                } else {
                    response.sendError(405);
                }
            } else {
                response.sendError(404);
            }
        } else {
            super.service(request, response);
        }
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

    private File tempDir = null;

    public File getTempDir() {
        if (tempDir == null) {
            File tempDir = (File) this.config.getServletContext().getAttribute(JAVAX_SERVLET_CONTEXT_TEMPDIR);
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
     * @param module the module the command belongs to
     * @param name command verb for command
     * @param commandObject object implementing the command
     * @return true if command was loaded and registered successfully
     */
    protected boolean registerOneCommand(ButterflyModule module, String name, Command commandObject) {
        return registerOneCommand(module.getName() + "/" + name, commandObject);
    }
    
    /**
     * Register a single command.
     *
     * @param path path for command
     * @param commandObject object implementing the command
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
     * @param module the module the command belongs to
     * @param name command verb for command
     * @param commandObject object implementing the command
     *            
     * @return true if command was loaded and registered successfully
     */
    static public boolean registerCommand(ButterflyModule module, String commandName, Command commandObject) {
        return s_singleton.registerOneCommand(module, commandName, commandObject);
    }
    
    static public Class<?> getClass(String className) throws ClassNotFoundException {
        if (className.startsWith("com.metaweb.")) {
            className = "com.google." + className.substring("com.metaweb.".length());
        }
        return Class.forName(className);
    }
}