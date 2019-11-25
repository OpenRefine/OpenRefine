package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snaccooperative.connection.SNACConnector;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.commands.*;
import com.google.refine.util.ParsingUtilities;

import org.snaccooperative.data.EntityId;

public class PreviewSNACSchemaCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EntityId testEntityId = new EntityId();
        String uri = "12345";
        testEntityId.setURI(uri);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
