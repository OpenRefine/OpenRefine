package com.google.refine.commands.importing;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.refine.model.meta.ImportSource;

public class ImportJob {
    static public enum State {
        NEW,
        RETRIEVING_DATA,
        READY,
        ERROR
    }
    
    final public long id;
    final public File dir;
    
    public long lastTouched;
    public State state = State.NEW;
    
    // Data for retrieving phase
    public int retrievingProgress = 0; // from 0 to 100
    public long bytesSaved = 0; // in case percentage is unknown
    public String errorMessage;
    public Throwable exception;
    
    public ImportSource importSource;
    
    public ImportJob(long id, File dir) {
        this.id = id;
        this.dir = dir;
        
        dir.mkdirs();
    }
    
    public void touch() {
        lastTouched = System.currentTimeMillis();
    }
    
    public void dispose() {
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
        }
    }
}
