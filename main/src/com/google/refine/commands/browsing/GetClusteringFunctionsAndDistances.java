package com.google.refine.commands.browsing;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;

public class GetClusteringFunctionsAndDistances extends Command {
	final static Logger logger = LoggerFactory.getLogger("get-clustering-functions-and-distances_command");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	
    }
}
