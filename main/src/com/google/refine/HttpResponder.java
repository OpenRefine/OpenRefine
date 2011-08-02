package com.google.refine;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpResponder {
    public void init(RefineServlet servlet);

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException;

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException;
}
