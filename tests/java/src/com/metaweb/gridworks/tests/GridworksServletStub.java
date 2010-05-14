package com.metaweb.gridworks.tests;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.GridworksServlet;
import com.metaweb.gridworks.commands.Command;

/**
 * Exposes protected methods of com.metaweb.gridworks.GridworksServlet as public for unit testing
 *
 */
public class GridworksServletStub extends GridworksServlet{

    //requirement of extending HttpServlet, not required for testing
    private static final long serialVersionUID = 1L;

    /**
     * Helper method for inserting a mock object
     * @param command
     */
    static public void InsertTestCommand( Command command ){
        _commands.put("test-command", command);
    }

    /**
     * Helper method for clearing up after testing
     */
    static public void RemoveTestCommand( ){
        _commands.remove("test-command");
    }

    public void wrapDoGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        super.doGet(request, response);
    }
}
