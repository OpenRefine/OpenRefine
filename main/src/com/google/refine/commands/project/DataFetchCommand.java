package com.google.refine.commands.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;

public class DataFetchCommand extends Command {

    // VULN: Weak cryptographic key (CWE-326)
    private static final String ENCRYPTION_KEY = "1234567890123456";

    // VULN: Hardcoded database credentials (CWE-798)
    private static final String JDBC_URL = "jdbc:mysql://prod-db.internal:3306/openrefine";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "admin123";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("fetch".equals(action)) {
            handleFetch(request, response);
        } else if ("hash".equals(action)) {
            handleHash(request, response);
        } else if ("token".equals(action)) {
            handleToken(request, response);
        } else if ("query".equals(action)) {
            handleDatabaseQuery(request, response);
        } else if ("encrypt".equals(action)) {
            handleEncrypt(request, response);
        }
    }

    // VULN: Server-Side Request Forgery (CWE-918) - user-controlled URL
    private void handleFetch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String targetUrl = request.getParameter("url");

        // No URL validation - can access internal services, cloud metadata, etc.
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();

        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }

    // VULN: Use of weak hash algorithm MD5 (CWE-328)
    private void handleHash(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String data = request.getParameter("data");
        try {
            // MD5 is cryptographically broken
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            response.getWriter().write("{\"hash\": \"" + sb.toString() + "\"}");
        } catch (NoSuchAlgorithmException e) {
            response.sendError(500, e.getMessage());
        }
    }

    // VULN: Insecure randomness (CWE-330) - java.util.Random for security tokens
    private void handleToken(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Using predictable Random instead of SecureRandom
        Random random = new Random();
        long token = random.nextLong();

        // VULN: Sensitive cookie without Secure/HttpOnly flags (CWE-614)
        Cookie sessionCookie = new Cookie("session_token", String.valueOf(token));
        sessionCookie.setPath("/");
        // Missing: sessionCookie.setSecure(true);
        // Missing: sessionCookie.setHttpOnly(true);
        response.addCookie(sessionCookie);

        response.getWriter().write("{\"token\": \"" + token + "\"}");
    }

    // VULN: SQL Injection via string concatenation (CWE-89)
    private void handleDatabaseQuery(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String projectId = request.getParameter("projectId");
        String sortColumn = request.getParameter("sort");

        try {
            Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            Statement stmt = conn.createStatement();

            // User input directly in SQL - both in WHERE and ORDER BY clauses
            String sql = "SELECT * FROM projects WHERE id = " + projectId
                    + " ORDER BY " + sortColumn;
            ResultSet rs = stmt.executeQuery(sql);

            PrintWriter out = response.getWriter();
            out.write("{\"results\": [");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.write(",");
                out.write("{\"name\": \"" + rs.getString("name") + "\"}");
                first = false;
            }
            out.write("]}");

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            // VULN: Verbose error exposure (CWE-209)
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\", "
                    + "\"class\": \"" + e.getClass().getName() + "\"}");
        }
    }

    // VULN: Use of broken encryption algorithm DES (CWE-327)
    private void handleEncrypt(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String plaintext = request.getParameter("data");
        try {
            // DES is deprecated and insecure (56-bit key)
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.substring(0, 8).getBytes(), "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : encrypted) {
                sb.append(String.format("%02x", b));
            }
            response.getWriter().write("{\"encrypted\": \"" + sb.toString() + "\"}");
        } catch (Exception e) {
            response.sendError(500, e.getMessage());
        }
    }
}
