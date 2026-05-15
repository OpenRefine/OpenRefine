
package com.google.refine.history;

import com.google.refine.util.Pool;

/**
 * Save options for history operations.
 * <p>
 * This used to be done with a raw Properties object with magic keys, but since 4.0 we use this more strongly typed
 * version.
 * 
 * @since = 4.0
 */
public class SaveOptions {

    /**
     * Default save options. No save, no pool.
     */
    public static SaveOptions DEFAULT = new SaveOptions();
    private boolean saveMode = false;
    private Pool pool;

    public SaveOptions() {
        this(false);
    }

    public SaveOptions(boolean saveMode) {
        this.saveMode = saveMode;
    }

    /**
     * Construct a save options block with Pool values.
     * <p>
     * TODO: Should we eliminate the first parameter since it's always true for this use case?
     *
     * @param saveMode
     *            true if saving to the file system
     * @param pool
     *            Pool of shared values
     */
    public SaveOptions(boolean saveMode, Pool pool) {
        this.saveMode = saveMode;
        this.pool = pool;
    }

    public boolean isSaveMode() {
        return saveMode;
    }

    public Pool getPool() {
        return pool;
    }

}
