package com.google.refine.commands.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.google.refine.commands.Command;

public class FileUploadCommand extends Command {

    // VULN: Hardcoded credentials (CWE-798)
    private static final String DB_USERNAME = "admin";
    private static final String DB_PASSWORD = "s3cretP@ssw0rd!";
    private static final String API_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // VULN: Reflected XSS (CWE-79) - user input written directly to response
        String fileName = request.getParameter("fileName");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>File: " + fileName + "</h1>");
        out.println("</body></html>");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        switch (action) {
            case "upload":
                handleFileUpload(request, response);
                break;
            case "read":
                handleFileRead(request, response);
                break;
            case "execute":
                handleCommandExecution(request, response);
                break;
            case "parse":
                handleXmlParse(request, response);
                break;
            case "deserialize":
                handleDeserialization(request, response);
                break;
            case "load":
                handleDynamicLoad(request, response);
                break;
            default:
                response.sendError(400, "Unknown action");
        }
    }

    // VULN: Path Traversal (CWE-22) - user-controlled file path without validation
    private void handleFileUpload(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String targetDir = request.getParameter("directory");
        String fileName = request.getParameter("fileName");

        // No path validation - allows ../../etc/passwd style attacks
        File targetFile = new File(targetDir, fileName);
        FileOutputStream fos = new FileOutputStream(targetFile);
        InputStream is = request.getInputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.close();
        response.getWriter().write("File uploaded to: " + targetFile.getAbsolutePath());
    }

    // VULN: Path Traversal in file read (CWE-22)
    private void handleFileRead(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String filePath = request.getParameter("path");

        // Directly using user input as file path
        Path path = Paths.get(filePath);
        byte[] content = Files.readAllBytes(path);
        response.setContentType("application/octet-stream");
        response.getOutputStream().write(content);
    }

    // VULN: Command Injection (CWE-78) - user input passed to Runtime.exec
    private void handleCommandExecution(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String filename = request.getParameter("filename");

        // User input directly concatenated into shell command
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec("file " + filename);

        InputStream is = proc.getInputStream();
        byte[] output = is.readAllBytes();
        response.getWriter().write(new String(output));
    }

    // VULN: XXE - XML External Entity Injection (CWE-611)
    private void handleXmlParse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            // DocumentBuilderFactory without disabling external entities
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // No protection against XXE:
            // dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(request.getInputStream());
            response.getWriter().write("Parsed XML root: " + doc.getDocumentElement().getTagName());
        } catch (Exception e) {
            response.sendError(500, e.getMessage());
        }
    }

    // VULN: Insecure Deserialization (CWE-502)
    private void handleDeserialization(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Deserializing untrusted user input - can lead to RCE
        ObjectInputStream ois = new ObjectInputStream(request.getInputStream());
        try {
            Object obj = ois.readObject();
            response.getWriter().write("Deserialized object: " + obj.toString());
        } catch (ClassNotFoundException e) {
            response.sendError(500, e.getMessage());
        } finally {
            ois.close();
        }
    }

    // VULN: Unsafe Reflection (CWE-470) - user-controlled class loading
    private void handleDynamicLoad(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String className = request.getParameter("className");
        try {
            // Loading arbitrary classes based on user input
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            response.getWriter().write("Loaded: " + instance.getClass().getName());
        } catch (Exception e) {
            response.sendError(500, e.getMessage());
        }
    }
}
