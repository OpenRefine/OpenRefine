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

package com.google.refine.commands.expr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.TopList;

public class GetExpressionHistoryCommand extends Command {
    
    public static class ExpressionState  {
        @JsonProperty("code")
        protected String code;
        @JsonProperty("global")
        protected boolean global = false;
        @JsonProperty("starred")
        protected boolean starred;
        
        protected ExpressionState(String code, boolean starred) {
            this.code = code;
            this.starred = starred;
        }
    }
    
    public static class ExpressionsList  {
        @JsonProperty("expressions")
        List<ExpressionState> expressions;
        
        protected ExpressionsList(List<ExpressionState> states) {
            this.expressions = states;
        }
    }

    static protected List<String> toExpressionList(Object o) {
        return o == null ? new ArrayList<String>() : ((TopList) o).getList();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            List<String> expressions = toExpressionList(ProjectManager.singleton.getPreferenceStore().get("scripting.expressions"));
            TopList topList = (TopList)ProjectManager.singleton.getPreferenceStore().get("scripting.starred-expressions");
            if (topList == null) {
            	topList = new TopList(ProjectManager.EXPRESSION_HISTORY_MAX);
            }
			Set<String> starredExpressions = new HashSet<String>(topList.getList());
            ExpressionsList expressionsList = new ExpressionsList(expressions.stream()
                    .map(s -> new ExpressionState(s, starredExpressions.contains(s)))
                    .collect(Collectors.toList()));
      
            respondJSON(response, expressionsList);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
