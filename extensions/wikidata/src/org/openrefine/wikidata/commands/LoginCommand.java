/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.commands;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;
import org.openrefine.wikidata.editing.ConnectionManager;

import com.google.refine.commands.Command;

public class LoginCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("wb-username");
        String password = request.getParameter("wb-password");
        String remember = request.getParameter("remember-credentials");
        ConnectionManager manager = ConnectionManager.getInstance();
        if (username != null && password != null) {
            manager.login(username, password, "on".equals(remember));
        } else if ("true".equals(request.getParameter("logout"))) {
            manager.logout();
        }
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        StringWriter sb = new StringWriter(2048);
        JSONWriter writer = new JSONWriter(sb);

        try {
            writer.object();
            writer.key("logged_in");
            writer.value(manager.isLoggedIn());
            writer.key("username");
            writer.value(manager.getUsername());
            writer.endObject();
        } catch (JSONException e) {
            logger.error(e.getMessage());
        }
        respond(response, sb.toString());
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
