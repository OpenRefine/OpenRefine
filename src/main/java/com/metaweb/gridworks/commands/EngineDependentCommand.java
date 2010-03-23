package com.metaweb.gridworks.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.Process;

/**
 * Convenient super class for commands that perform abstract operations on
 * only the filtered rows based on the faceted browsing engine's configuration
 * on the client side. 
 * 
 * The engine's configuration is passed over as a POST body parameter. It is 
 * retrieved, de-serialized, and used to construct the abstract operation.
 * The operation is then used to construct a process. The process is then
 * queued for execution. If the process is not long running and there is no
 * other queued process, then it gets executed right away, resulting in some
 * change to the history. Otherwise, it is pending. The client side can
 * decide how to update its UI depending on whether the process is done or
 * still pending.
 * 
 * Note that there are interactions on the client side that change only 
 * individual cells or individual rows (such as starring one row or editing 
 * the text of one cell). These interactions do not depend on the faceted
 * browsing engine's configuration, and so they don't invoke commands that
 * subclass this class. See AnnotateOneRowCommand and EditOneCellCommand as
 * examples. 
 */
abstract public class EngineDependentCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            AbstractOperation op = createOperation(project, request, getEngineConfig(request));
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    abstract protected AbstractOperation createOperation(
            Project project, HttpServletRequest request, JSONObject engineConfig) throws Exception;
}
