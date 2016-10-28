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

package com.google.refine.model.changes;

import java.io.IOException;
import com.google.common.collect.Lists;
 
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.history.History;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class MassChange implements Change {
    final protected List<? extends Change> _changes;
    final protected boolean                _updateRowContextDependencies;
    
    public MassChange(List<? extends Change> changes, boolean updateRowContextDependencies) {
        _changes = changes;
        _updateRowContextDependencies = updateRowContextDependencies;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
            for (Change change : _changes) {
                change.apply(project);
            }
            
            if (_updateRowContextDependencies) {
                project.update();
            }
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            for (Change change : Lists.reverse(_changes)){
                change.revert(project);
            }
            
            if (_updateRowContextDependencies) {
                project.update();
            }
        }
    }
    
    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("updateRowContextDependencies="); writer.write(Boolean.toString(_updateRowContextDependencies)); writer.write('\n');
        writer.write("changeCount="); writer.write(Integer.toString(_changes.size())); writer.write('\n');
        for (Change c : _changes) {
            History.writeOneChange(writer, c, options);
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        boolean updateRowContextDependencies = false;
        List<Change> changes = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("updateRowContextDependencies".equals(field)) {
                updateRowContextDependencies = Boolean.parseBoolean(line.substring(equal + 1));
            } else if ("changeCount".equals(field)) {
                int changeCount = Integer.parseInt(line.substring(equal + 1));
                
                changes = new ArrayList<Change>(changeCount);
                for (int i = 0; i < changeCount; i++) {
                    changes.add(History.readOneChange(reader, pool));
                }
            }
        }
        
        MassChange change = new MassChange(changes, updateRowContextDependencies);
        
        return change;
    }
}
