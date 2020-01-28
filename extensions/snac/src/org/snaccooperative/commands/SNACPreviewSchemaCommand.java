package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;

import org.snaccooperative.exporters.SNACResourceCreator;

public class SNACPreviewSchemaCommand extends Command  {

   // @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        doGet(request,response);

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SNACResourceCreator manager = SNACResourceCreator.getInstance();
        String previewString = manager.obtainPreview();
        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        writer.writeStartObject();
        writer.writeStringField("SNAC_preview", previewString);
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();

    }
}
