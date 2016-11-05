package org.dtls.fairifier;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.google.refine.commands.Command;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.reconcile.util.StringUtils;
import org.json.JSONWriter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Iterator;
import java.util.Map.Entry;
import org.json.JSONException;
import java.util.TreeMap;

/**
 * @author Shamanou van Leeuwen
 * @date 31-10-2016
 *
 */
public class LanguageGetterCommand extends Command{
    private static final String URL = "http://id.loc.gov/vocabulary/iso639-1.madsrdf.json";
    private static final String USER_AGENT = "FAIRifier/0.1";
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try{
            Iterator<Entry<String,String>> it =  new TreeMap<String, String>(this.getLanguages()).entrySet().iterator();
            ArrayList out = new ArrayList<>();
            while (it.hasNext()){
                Map.Entry<String,String> pair = (Map.Entry)it.next();
                String[] tmp = new String[2];
                tmp[0] = pair.getKey();
                tmp[1] = pair.getValue();
                out.add(tmp);
            }
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("content"); writer.value(out);
            writer.endObject();
        } catch(Exception e){
            respondException(response, e);
        }
    }
    
    public Map<String,String> getLanguages() throws JSONException, IOException{
           HashMap<String,String> map = new HashMap<String,String>();
           BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(URL).getContent()));
           StringBuilder out = new StringBuilder();
           String line;
           while ((line = reader.readLine()) != null) {
               out.append(line);
           }
           reader.close();
           JSONObject jsonObject = new JSONObject("{json:" + out.toString() + "}");
           JSONArray array = jsonObject.getJSONArray("json").getJSONObject(0).getJSONArray("http://www.loc.gov/mads/rdf/v1#hasTopMemberOfMADSScheme");
           for (int i = 0; i < array.length(); i++) {
               String url = array.getJSONObject(i).getString("@id");
               map.put(url.substring(url.length() - 2 ), url);
           }
           return map;
    }
}
