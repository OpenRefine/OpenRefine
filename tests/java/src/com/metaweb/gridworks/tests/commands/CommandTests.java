package com.metaweb.gridworks.tests.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.Project;

public class CommandTests {

    final static protected Logger logger = LoggerFactory.getLogger("CommandTests");

    CommandStub SUT = null;
    HttpServletRequest request = null;
    ProjectManager projectManager = null;
    Project project = null;

    @Before
    public void SetUp() {
        SUT = new CommandStub();
        request = mock(HttpServletRequest.class);
        projectManager = mock(ProjectManager.class);
        project = mock(Project.class);
    }

    @After
    public void TearDown() {
        SUT = null;
        request = null;
        projectManager = null;
        project = null;
    }

    // -----------------getProject tests------------

    @Test
    public void getProjectThrowsWithNullParameter() {
        try {
            SUT.wrapGetProject(null);
            Assert.fail(); // should throw exception before this
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void getProjectThrowsIfResponseHasNoOrBrokenProjectParameter() {
        when(request.getParameter("project")).thenReturn(""); // null
        try {
            SUT.wrapGetProject(request);
        } catch (ServletException e) {
            // expected
        } catch (Exception e) {
            Assert.fail();
        }
        verify(request, times(1)).getParameter("project");
    }

    // -----------------getEngineConfig tests-----------------
    @Test
    public void getEngineConfigThrowsWithNullParameter() {
        try {
            SUT.wrapGetEngineConfig(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void getEngineConfigReturnsNullWithNullEngineParameter() {
        when(request.getParameter("engine")).thenReturn(null);
        try {
            Assert.assertNull(SUT.wrapGetEngineConfig(request));
        } catch (JSONException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void getEngineConfigReturnsNullWithEmptyOrBadParameterValue() {
        when(request.getParameter("engine")).thenReturn("sdfasdfas");

        try {
            Assert.assertNull( SUT.wrapGetEngineConfig(request) );
        } catch (JSONException e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter("engine");
    }

    @Test
    public void getEngineConfigRegressionTest() {
        when(request.getParameter("engine")).thenReturn("{\"hello\":\"world\"}");
        JSONObject o = null;
        try {
            o = SUT.wrapGetEngineConfig(request);
            Assert.assertEquals("world", o.getString("hello"));
        } catch (JSONException e) {
            Assert.fail();
        } catch (Exception e) {
            Assert.fail();
        }
        verify(request, times(1)).getParameter("engine");
    }

    // -----------------getEngine tests----------------------
    @Test
    public void getEngineThrowsOnNullParameter() {
        try {
            SUT.wrapGetEngine(null, null);
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            Assert.fail();
        }

        try {
            SUT.wrapGetEngine(null, project);
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            Assert.fail();
        }

        try {
            SUT.wrapGetEngine(request, null);
        } catch (IllegalArgumentException e) {
            // expected
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void getEngineRegressionTest() {
        // TODO refactor getEngine to use dependency injection, so a mock Engine
        // object can be used.

        Engine engine = null;
        when(request.getParameter("engine")).thenReturn("{\"hello\":\"world\"}");

        try {
            engine = SUT.wrapGetEngine(request, project);
            Assert.assertNotNull(engine);
        } catch (Exception e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter("engine");
        // JSON configuration doesn't have 'facets' key or 'INCLUDE_DEPENDENT'
        // key, so there should be no further action
        // Engine._facets is protected so can't test that it is of zero length.
    }

    // ------------------
    @Test
    public void getIntegerParameterWithNullParameters() {
        // all null
        try {
            SUT.wrapGetIntegerParameter(null, null, 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        // request null
        try {
            SUT.wrapGetIntegerParameter(null, "name", 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void getIntegerParametersWithIncorrectParameterName() {

        when(request.getParameter(null)).thenReturn(null);
        when(request.getParameter("incorrect")).thenReturn(null);

        // name null
        try {
            int returned = SUT.wrapGetIntegerParameter(request, null, 5);
            Assert.assertEquals(5, returned);
        } catch (IllegalArgumentException e) {
            Assert.fail();
        }

        // name incorrect
        try {
            int returned = SUT.wrapGetIntegerParameter(request, "incorrect", 5);
            Assert.assertEquals(5, returned);
        } catch (IllegalArgumentException e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter(null);
        verify(request, times(1)).getParameter("incorrect");
    }

    @Test
    public void getIntegerParametersRegressionTest() {
        when(request.getParameter("positivenumber")).thenReturn("22");
        when(request.getParameter("zeronumber")).thenReturn("0");
        when(request.getParameter("negativenumber")).thenReturn("-40");

        // positive
        try {
            int returned = SUT.wrapGetIntegerParameter(request,"positivenumber", 5);
            Assert.assertEquals(22, returned);
        } catch (IllegalArgumentException e) {
            Assert.fail();
        }

        // zero
        try {
            int returned = SUT.wrapGetIntegerParameter(request, "zeronumber", 5);
            Assert.assertEquals(0, returned);
        } catch (IllegalArgumentException e) {
            Assert.fail();
        }

        // negative
        try {
            int returned = SUT.wrapGetIntegerParameter(request,
                    "negativenumber", 5);
            Assert.assertEquals(-40, returned);
        } catch (IllegalArgumentException e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter("positivenumber");
        verify(request, times(1)).getParameter("zeronumber");
        verify(request, times(1)).getParameter("negativenumber");
    }

    // ---------------------getJsonParameter tests----------------
    @Test
    public void getJsonParameterWithNullParameters() {
        when(request.getParameter(null)).thenReturn(null);
        when(request.getParameter("")).thenReturn(null);

        try {
            SUT.wrapGetJsonParameter(null, null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        Assert.assertNull(SUT.wrapGetJsonParameter(request, null));

        try {
            SUT.wrapGetJsonParameter(null, "test");
        } catch (IllegalArgumentException e) {
            // expected
        }

        Assert.assertNull(SUT.wrapGetJsonParameter(request, ""));

        verify(request, times(1)).getParameter(null);
        verify(request, times(1)).getParameter("");
    }

    @Test
    public void getJsonParameterRegressionTest() {
        when(request.getParameter("test")).thenReturn("{\"foo\":\"bar\"}");

        JSONObject o = SUT.wrapGetJsonParameter(request, "test");
        Assert.assertNotNull(o);
        try {
            Assert.assertEquals("bar", o.getString("foo"));
        } catch (JSONException e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter("test");
    }

    @Test
    public void getJsonParameterWithMalformedJson() {
        when(request.getParameter("test")).thenReturn("brokenJSON");

        try {
            Assert.assertNull(SUT.wrapGetJsonParameter(request, "test"));
        } catch (Exception e) {
            Assert.fail();
        }

        verify(request, times(1)).getParameter("test");
    }
}
