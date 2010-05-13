package com.metaweb.gridworks.tests.commands;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;

/**
 * Implementation of abstract class for testing Exposes protected members as public
 */
@Ignore
public class CommandStub extends Command {

    public Project wrapGetProject(HttpServletRequest request)
    throws ServletException {
        return getProject(request);
    }

    public JSONObject wrapGetEngineConfig(HttpServletRequest request)
    throws JSONException {
        return getEngineConfig(request);
    }

    public Engine wrapGetEngine(HttpServletRequest request, Project project)
    throws Exception {
        return getEngine(request, project);
    }

    public int wrapGetIntegerParameter(HttpServletRequest request, String name,int def) {
        return getIntegerParameter(request, name, def);
    }

    public JSONObject wrapGetJsonParameter(HttpServletRequest request,String name) {
        return getJsonParameter(request, name);
    }
}
