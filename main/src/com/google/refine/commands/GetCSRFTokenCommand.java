
package com.google.refine.commands;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Generates a fresh CSRF token.
 */
public class GetCSRFTokenCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        respondJSON(response, Collections.singletonMap("token", csrfFactory.getFreshToken()));
    }
}
