package org.dtls.fairifier;

import java.io.InputStreamReader;
import java.io.BufferedReader;
//import org.deri.grefine.rdf.app.ApplicationContext;
import com.google.refine.commands.Command;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.json.JSONWriter;


/**
 * @author Shamanou van Leeuwen
 * @date 31-10-2016
 *
 */
public class LicenseGetterCommand extends Command{
    private static final String URL = "http://rdflicense.appspot.com/rdflicense";
    private static final String USER_AGENT = "FAIRifier/0.1";
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try{
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("content"); writer.value(this.getLicenses());
            writer.endObject();
        }catch(Exception e){
            respondException(response, e);
        }
    }
    
    public String getLicenses() throws IOException{
           BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(URL).getContent()));
           StringBuilder out = new StringBuilder();
           String line;
           while ((line = reader.readLine()) != null) {
               out.append(line);
           }
           reader.close();
           return out.toString();
    }
}
