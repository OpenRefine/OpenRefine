package org.dtls.fairifier;

import org.deri.grefine.rdf.utils.HttpUtils;
import org.deri.grefine.reconcile.model.ReconciliationStanbolSite;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.google.refine.commands.Command;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONWriter;
import com.google.refine.commands.Command;

/**
 * 
 * @author Shamanou van Leeuwen
 * @date 1-11-2016
 *
 */

public class GetFairDataPointCommand extends Command{
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String uri = req.getParameter("uri");
        try{
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("content"); writer.value(this.getFDP(uri));
            writer.endObject();
        }catch(Exception e){
            respondException(res, e);
        }
    }
    
    public String getFDP(String url) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url).getContent()));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }
}