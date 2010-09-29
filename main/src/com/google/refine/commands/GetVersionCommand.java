package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.RefineServlet;

public class GetVersionCommand extends Command {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "text/plain");
            response.getWriter().write(RefineServlet.VERSION);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
}
