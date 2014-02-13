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

package com.google.refine.io;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;


public class ProjectMetadataUtilities {
    final static Logger logger = LoggerFactory.getLogger("project_metadata_utilities");
    
    /**
     * Reconstruct the project metadata on a best efforts basis.  The name is
     * gone, so build something descriptive from the column names.  Recover the
     * creation and modification times based on whatever files are available.
     * 
     * @param project
     * @return
     */
    static public ProjectMetadata recover(Project project) {
        ProjectMetadata pm = null;
        
        if (project.load()) {
            List<String> columnNames = project.columnModel.getColumnNames();
            String tempName = "<recovered project> - " + columnNames.size() 
                    + " cols X " + project.rows.size() + " rows - "
                    + StringUtils.join(columnNames,'|');
            
            Project.ProjectInfo projectInfo = project.getInfo();
            
            pm = new ProjectMetadata(new Date(projectInfo.created),new Date(projectInfo.modified), tempName);
            logger.error("Partially recovered missing metadata project in directory " + projectInfo.location + " - " + tempName);
        }
        return pm;
    }
}
