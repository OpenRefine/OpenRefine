package com.google.gridworks.tests;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gridworks.GridworksServlet;
import com.google.gridworks.commands.Command;

/**
 * Exposes protected methods of com.google.gridworks.GridworksServlet as public for unit testing
 *
 */
public class GridworksServletStub extends GridworksServlet {

    //requirement of extending HttpServlet, not required for testing
    private static final long serialVersionUID = 1L;

    public void wrapService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        super.service(request, response);
    }

    public String wrapGetCommandName(HttpServletRequest request){
        return super.getCommandKey(request);
    }

    //-------------------helper methods--------------
    /**
     * Helper method for inserting a mock object
     * @param commandName
     * @param command
     */
    public void insertCommand(String commandName, Command command ){
        registerOneCommand("core/" + commandName, command);
    }

    /**
     * Helper method for clearing up after testing
     * @param commandName
     */
    public void removeCommand( String commandName ){
        unregisterCommand(commandName);
    }
}
