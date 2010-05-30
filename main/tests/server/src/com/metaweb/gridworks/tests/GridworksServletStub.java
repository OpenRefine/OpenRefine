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
public class GridworksServletStub extends GridworksServlet {

    //requirement of extending HttpServlet, not required for testing
    private static final long serialVersionUID = 1L;

    public void wrapDoGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        super.doGet(request, response);
    }

    public void wrapDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        super.doPost(request, response);
    }

    public String wrapGetCommandName(HttpServletRequest request){
        return super.getCommandName(request);
    }

    //-------------------helper methods--------------
    /**
     * Helper method for inserting a mock object
     * @param commandName
     * @param command
     */
    static public void InsertCommand( String commandName, Command command ){
        registerCommand(commandName, command);
    }

    /**
     * Helper method for clearing up after testing
     * @param commandName
     */
    static public void RemoveCommand( String commandName ){
        unregisterCommand(commandName);
    }
}
