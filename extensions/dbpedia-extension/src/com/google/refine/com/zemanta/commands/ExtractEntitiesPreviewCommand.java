package com.google.refine.com.zemanta.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.com.zemanta.util.ZemantaUtil;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;

public class ExtractEntitiesPreviewCommand extends Command {

        static final Logger logger = LoggerFactory.getLogger("preview-extract-entities");
        protected int cellIndex;
        protected StringBuffer cellsText;
        static int PREVIEW_SIZE = 5;

        public void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                try {

                        String text = request.getParameter("text");

                        String apiKey = (String)ZemantaUtil.getPreference("zemanta.apikey");            
                        String storedTimeout = (String)ZemantaUtil.getPreference("zemantaService.timeout");
                        int timeout = storedTimeout.equals("")? 2000 : Integer.parseInt(storedTimeout);

                        InputStream is = getDataFromZemantaAPI(apiKey, text, timeout);
                        String raw = ParsingUtilities.inputStreamToString(is);
                        respond(response,raw);


                } catch (IOException e) {
                        System.out.println("IOException: " + e.getLocalizedMessage());
                        respondException(response, e);
                }

                catch (Exception e) {

                        respondException(response,e);
                }

        }

        static protected InputStream getDataFromZemantaAPI(String apiKey, String text, int timeout) throws IOException {

                String connString ="http://api.zemanta.com/services/rest/0.0/";
                StringBuffer data = new StringBuffer();

                data.append("method=zemanta.suggest_markup&")
                .append("api_key=").append(apiKey).append("&")
                .append("format=json&")
                .append("freebase=").append("1").append("&")
                .append("text=").append(URLEncoder.encode(text, "UTF8"));

                URL url = new URL(connString);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(timeout);
                connection.setDoOutput(true);
                connection.addRequestProperty("Content-Length", Integer.toString(data.length()));
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                dos.write(data.toString().getBytes());
                dos.close();
                connection.connect();

                return connection.getInputStream();


        }


}
