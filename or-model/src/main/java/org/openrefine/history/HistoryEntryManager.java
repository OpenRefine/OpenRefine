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

package org.openrefine.history;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.FileChangeDataStore;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A utility class to load and save project histories.
 *
 */
public class HistoryEntryManager {
	
	protected static final String INITIAL_GRID_SUBDIR = "initial";
	protected static final String METADATA_FILENAME = "history.json";
	protected static final String CHANGE_SUBDIR = "changes";
	
	private final DatamodelRunner runner;
	
	public HistoryEntryManager(DatamodelRunner runner) {
		this.runner = runner;
	}
	
    /**
     * Saves the history and the initial grid state to a directory.
     * @param dir
     * 		the directory where the history should be saved.
     * @throws IOException 
     */
    public void save(History history, File dir) throws IOException {
    	File gridFile = new File(dir, INITIAL_GRID_SUBDIR);
    	File metadataFile = new File(dir, METADATA_FILENAME);
    	// Save the initial grid if does not exist yet (it is immutable)
    	if(!gridFile.exists()) {
    	    history.getInitialGridState().saveToFile(gridFile);
    	}
    	Metadata metadata = new Metadata();
    	metadata.entries = history.getEntries();
    	metadata.position = history.getPosition();
    	// Save the metadata
    	ParsingUtilities.saveWriter.writeValue(metadataFile, metadata);
    }
    
    public History load(File dir) throws IOException, DoesNotApplyException {
    	File gridFile = new File(dir, INITIAL_GRID_SUBDIR);
    	File metadataFile = new File(dir, METADATA_FILENAME);
    	// Load the metadata
    	Metadata metadata = ParsingUtilities.mapper.readValue(metadataFile, Metadata.class);
    	// Load the initial grid
    	GridState gridState = runner.loadGridState(gridFile);
    	// Set up the file-based change data store
    	ChangeDataStore dataStore = new FileChangeDataStore(runner, new File(dir, CHANGE_SUBDIR));
    	return new History(gridState, dataStore, metadata.entries, metadata.position);
    }
    
    /**
     * Utility class to help with Jackson deserialization
     *
     */
    protected static class Metadata {
    	@JsonProperty("entries")
        protected List<HistoryEntry> entries;
        @JsonProperty("position")
        int position;
    }
}
