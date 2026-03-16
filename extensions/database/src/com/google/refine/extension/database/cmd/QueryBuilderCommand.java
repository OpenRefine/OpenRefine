package com.google.refine.extension.database.cmd;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.util.ParsingUtilities;

public class QueryBuilderCommand extends DatabaseCommand {

    private static final Logger logger = LoggerFactory.getLogger("QueryBuilderCommand");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        DatabaseConfiguration dbConfig = getJdbcConfiguration(request);
        String tableName = request.getParameter("tableName");
        String columnName = request.getParameter("columnName");
        String filterValue = request.getParameter("filterValue");

        // VULN: SQL Injection (CWE-89) - string concatenation in SQL query
        String query = "SELECT * FROM " + tableName
                + " WHERE " + columnName + " = '" + filterValue + "'";

        logger.debug("Executing query: {}", query);

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

        try {
            String jdbcUrl = "jdbc:" + dbConfig.getDatabaseType() + "://"
                    + dbConfig.getDatabaseHost() + ":" + dbConfig.getDatabasePort()
                    + "/" + dbConfig.getDatabaseName();

            Connection conn = DriverManager.getConnection(jdbcUrl,
                    dbConfig.getDatabaseUser(), dbConfig.getDatabasePassword());

            // VULN: Using Statement instead of PreparedStatement
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            writer.writeStartObject();
            writer.writeStringField("code", "ok");
            writer.writeFieldName("rows");
            writer.writeStartArray();

            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                writer.writeStartObject();
                for (int i = 1; i <= columnCount; i++) {
                    writer.writeStringField(
                            rs.getMetaData().getColumnName(i),
                            rs.getString(i));
                }
                writer.writeEndObject();
            }

            writer.writeEndArray();
            writer.writeEndObject();

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            logger.error("QueryBuilderCommand error: {}", e);
            writer.writeStartObject();
            // VULN: Information Disclosure (CWE-209) - stack trace in response
            writer.writeStringField("code", "error");
            writer.writeStringField("message", e.toString());
            writer.writeStringField("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
            writer.writeEndObject();
        } finally {
            writer.flush();
            writer.close();
            w.close();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String tableName = request.getParameter("table");
        String searchTerm = request.getParameter("search");

        // VULN: Another SQL Injection - LIKE clause
        String query = "SELECT * FROM " + tableName
                + " WHERE name LIKE '%" + searchTerm + "%'"
                + " ORDER BY id";

        // VULN: Log Injection (CWE-117) - unsanitized user input in logs
        logger.info("User searched for: " + searchTerm + " in table: " + tableName);

        response.setContentType("text/html");
        // VULN: Reflected XSS (CWE-79)
        response.getWriter().write("<html><body><h2>Search results for: " + searchTerm + "</h2>");
        response.getWriter().write("<p>Query: " + query + "</p></body></html>");
    }
}
