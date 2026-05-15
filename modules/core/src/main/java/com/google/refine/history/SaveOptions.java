/*******************************************************************************
 * Copyright (C) 2026, OpenRefine contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.history;

import com.google.refine.util.Pool;

/**
 * Save options for history operations.
 * <p>
 * This used to be done with a raw Properties object with magic keys, but since 4.0 we use this more strongly typed
 * version.
 * 
 * @since 4.0
 */
public class SaveOptions {

    /**
     * Default save options. No save, no pool.
     */
    public static final SaveOptions DEFAULT = new SaveOptions();
    private final boolean saveMode;
    private final Pool pool;

    public SaveOptions() {
        this(false, null);
    }

    public SaveOptions(boolean saveMode) {
        this(saveMode, null);
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
