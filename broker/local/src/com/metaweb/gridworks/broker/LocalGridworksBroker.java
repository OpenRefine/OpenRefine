package com.metaweb.gridworks.broker;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

public class LocalGridworksBroker extends GridworksBroker {
                
    protected static final Logger logger = LoggerFactory.getLogger("gridworks.broker.local");
    
    Environment env;
    EntityStore projectStore;
    EntityStore lockStore;
    
    PrimaryIndex<String,Project> projectById;
    PrimaryIndex<String,Lock> lockByProject;

    Timer timer;
    LockExpirer expirer;
    
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);

        timer = new Timer();
        expirer = new LockExpirer();
        timer.schedule(expirer, LOCK_EXPIRATION_CHECK_DELAY, LOCK_EXPIRATION_CHECK_DELAY);
        
        File dataPath = new File("data"); // FIXME: data should be configurable;

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        env = new Environment(dataPath, envConfig); 

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        projectStore = new EntityStore(env, "ProjectsStore", storeConfig);
        lockStore = new EntityStore(env, "LockStore", storeConfig);

        projectById = projectStore.getPrimaryIndex(String.class, Project.class);
        lockByProject = lockStore.getPrimaryIndex(String.class, Lock.class);
    }
    
    @Override
    public void destroy() throws Exception {
        super.destroy();
        
        if (projectStore != null) {
            projectStore.close();
        } 

        if (lockStore != null) {
            lockStore.close();
        } 
        
        if (env != null) {
            env.cleanLog();
            env.close();
        }
    }

    class LockExpirer extends TimerTask {
        public void run() {
            for (Lock lock : lockByProject.entities()) {
                if (lock.timestamp + LOCK_DURATION < System.currentTimeMillis()) {
                    try {
                        releaseLock(null, lock.pid, lock.uid, lock.id);
                    } catch (Exception e) {
                        logger.error("Exception while expiring lock for project '" + lock.pid + "'", e);
                    }
                }
            }
        }
    }
    
    // ---------------------------------------------------------------------------------

    protected HttpClient getHttpClient() {
        return new DefaultHttpClient();
    }
    
    // ---------------------------------------------------------------------------------
    
    protected void expireLocks(HttpServletResponse response) throws Exception {
        expirer.run();
        respond(response, OK);
    }
    
    protected void getLock(HttpServletResponse response, String pid) throws Exception {
        respond(response, lockToJSON(getLock(pid)));
    }

    protected void obtainLock(HttpServletResponse response, String pid, String uid) throws Exception {

        Transaction txn = env.beginTransaction(null, null);
        
        Lock lock = getLock(pid);
        if (lock == null) {
            try {
                lock = new Lock(Long.toHexString(txn.getId()), pid, uid);
                lockByProject.put(txn, lock);
                txn.commit();
            } finally {
                if (txn != null) {
                    txn.abort();
                    txn = null;
                }
            }
        }
                
        respond(response, lockToJSON(lock));
    }
    
    protected void releaseLock(HttpServletResponse response, String pid, String uid, String lid) throws Exception {

        Transaction txn = env.beginTransaction(null, null);

        Lock lock = getLock(pid);
        if (lock != null) {
            try {
                if (!lock.id.equals(lid)) {
                    throw new RuntimeException("Lock id doesn't match, can't release the lock");
                }
                if (!lock.uid.equals(uid)) {
                    throw new RuntimeException("User id doesn't match the lock owner, can't release the lock");
                }
                
                lockByProject.delete(pid);
                
                txn.commit();
            } finally {
                if (txn != null) {
                    txn.abort();
                    txn = null;
                }
            }
        }
                
        if (response != null) { // this because the expiration thread can call this method without a real response
            respond(response, OK);
        }
    }
    
    // ----------------------------------------------------------------------------------------------------
    
    protected void startProject(HttpServletResponse response, String pid, String uid, String lid, String data) throws Exception {
        
        Transaction txn = env.beginTransaction(null, null);

        checkLock(pid, uid, lid);
        
        if (projectById.contains(pid)) {
            throw new RuntimeException("Project '" + pid + "' already exists");
        }
        
        try {
            projectById.put(txn, new Project(pid, data));
            txn.commit();
        } finally {
            if (txn != null) {
                txn.abort();
                txn = null;
            }
        }
        
        respond(response, OK);
    }

    protected void addTransformations(HttpServletResponse response, String pid, String uid, String lid, List<String> transformations) throws Exception {

        Transaction txn = env.beginTransaction(null, null);
        
        checkLock(pid, uid, lid);
        
        Project project = getProject(pid);
        
        if (project == null) {
            throw new RuntimeException("Project '" + pid + "' not found");
        }
        
        try {
            project.transformations.addAll(transformations);
            txn.commit();
        } finally {
            if (txn != null) {
                txn.abort();
                txn = null;
            }
        }
        
        respond(response, OK);
    }

    // ---------------------------------------------------------------------------------
    
    protected void getProject(HttpServletResponse response, String pid) throws Exception {
        Project project = getProject(pid);

        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        writer.object();
            writer.key("data"); writer.value(project.data);
            writer.key("transformations"); 
            writer.array();
                for (String s : project.transformations) {
                    writer.value(s);
                }
            writer.endArray();
        writer.endObject();
        w.flush();
        w.close();
    }
    
    protected void getHistory(HttpServletResponse response, String pid, int tindex) throws Exception {
        Project project = getProject(pid);

        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        writer.object();
        writer.key("transformations"); 
        writer.array();
            int size = project.transformations.size();
            for (int i = tindex; i < size; i++) {
                writer.value(project.transformations.get(i));
            }
        writer.endArray();
        writer.endObject();
        w.flush();
        w.close();
    }

    // ---------------------------------------------------------------------------------

    Project getProject(String pid) {
        Project project = projectById.get(pid);
        if (project == null) {
            throw new RuntimeException("Project '" + pid + "' is not managed by this broker");
        }
        return project;
    }
        
    @Entity
    class Project {
        
        @PrimaryKey
        String pid;

        List<String> transformations = new ArrayList<String>(); 

        String data;

        Project(String pid, String data) {
            this.pid = pid;
            this.data = data;
        }
        
        @SuppressWarnings("unused")
        private Project() {}
    }

    // ---------------------------------------------------------------------------------
    
    Lock getLock(String pid) {
        return lockByProject.get(pid);
    }

    void checkLock(String pid, String uid, String lid) {
        Lock lock = getLock(pid);
    
        if (lock == null) {
            throw new RuntimeException("No lock was found with the given Lock id '" + lid + "', you have to have a valid lock on a project in order to start it");
        }
        
        if (!lock.pid.equals(pid)) {
            throw new RuntimeException("Lock '" + lid + "' is for another project: " + pid);
        }
        
        if (!lock.uid.equals(uid)) {
            throw new RuntimeException("Lock '" + lid + "' is owned by another user: " + uid);
        }
    }
    
    JSONObject lockToJSON(Lock lock) throws JSONException {
        JSONObject o = new JSONObject();
        if (lock != null) {
            o.put("lock_id", lock.id);
            o.put("project_id", lock.pid);
            o.put("user_id", lock.uid);
            o.put("timestamp", lock.timestamp);
        }
        return o;
    }
    
    @Entity
    class Lock {

        @PrimaryKey
        String pid;
        
        String id;
        
        String uid;
        
        long timestamp;
        
        Lock(String id, String pid, String uid) {
            this.id = id;
            this.pid = pid;
            this.uid = uid;
            this.timestamp = System.currentTimeMillis();
        }
                
        @SuppressWarnings("unused")
        private Lock() {}
    }
}
