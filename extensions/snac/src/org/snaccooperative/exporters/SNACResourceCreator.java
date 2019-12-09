package org.snaccooperative.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snaccooperative.connection.SNACConnector;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Cell;

import org.snaccooperative.data.Resource;
import org.snaccooperative.data.Term;
import org.snaccooperative.data.Constellation;
import org.snaccooperative.data.Language;


/*
Required Fields:
  - Title:
    - getTitle()
    - setTitle(String)
  - Link:
    - getLink()
    - setLink(String)
  - Document Type:
    - getDocumentType()
    = setDocumentType(Term)
  - Repository:
    - getRepository()
    - setRepository(Constellation)

Other (non-required) fields:
  - ID
  - Display Entry
    - getDisplayEntry()
    - setDisplayEntry(String)
  - Abstract
    - getAbstract()
    - setAbstract(String)
  - Extent
    - getExtent()
    - setExtent(String)
  - Date
    - getDate()
    - setDate(String)
  - Language
    - getLanguages()
    - setLanguages(List<Language>)
    - addLanguage(Language)
  - Note

Term Object (Hardcoded lol)
  - "id": "696"
    - getID()
    - setID(int)
  - "term": "Archival Resource"
    - getTerm()
    - setTerm(String)
  - "type": "document_type"
    - getType()
    - setType(String)

Constellation Object


*/

public class SNACResourceCreator {
    private static HashMap<String, String> match_attributes = new HashMap<String, String>();
    private static Project theProject = new Project();
    private static final SNACResourceCreator instance = new SNACResourceCreator();
    private static List<Resource> resources = new LinkedList<Resource>();
    private static List<String> csv_headers = new LinkedList<String>();

    // Internal Resource IDs that isn't part of the Resource data model

    private static List<Integer> resource_ids = new LinkedList<Integer>();
    private static HashMap<String, String[]> language_code = new HashMap<String, String[]>();

    public static SNACResourceCreator getInstance() {
        return instance;
    }

    public void updateColumnMatches(String JSON_SOURCE){
        try {
          match_attributes = new ObjectMapper().readValue(JSON_SOURCE, HashMap.class);
        } catch (IOException e) {
          e.printStackTrace();
        }
    }

    public String getColumnMatchesJSONString(){
        return new JSONObject(match_attributes).toString();
    }

    public static void setProject(Project p){
        theProject = p;
        csv_headers = theProject.columnModel.getColumnNames();
        // for (int x = 0; x < csv_headers.size(); x++){
        //    System.out.println(csv_headers.get(x));
        // }
    }

    public void setUp(Project p, String JSON_SOURCE){
        setProject(p);
        updateColumnMatches(JSON_SOURCE);
        rowsToResources();
        exportResourcesJSON();
    }

    /**
    * Take a given Row and convert it to a Resource Object
    *
    * @param row (Row found in List<Row> from Project)
    * @return Resource converted from Row
    */
    public Resource createResource(Row row){
        Resource res = new Resource();
        for (int x = 0; x < csv_headers.size(); x++){
            String snac_header = match_attributes.get(csv_headers.get(x)).toLowerCase();
            if (snac_header == null || snac_header == ""){
                continue;
            }
            // Insert switch statements || Bunch of if statements for setters
            String temp_val;
            // If cell empty, set value to empty value
            if (row.getCellValue(x) == null || row.getCellValue(x) == ""){
                // Should it be empty or continue without adding?
                temp_val = "";
                // continue;
            } else{
                temp_val = row.getCellValue(x).toString();
            }
            switch(snac_header){
              case "id":
                  try{
                      resource_ids.add(Integer.parseInt(temp_val));
                      // System.out.println("ID: " + temp_val);
                      break;
                  }
                  catch (NumberFormatException e){
                      break;
                  }
              case "type":
                  try{
                      Term t = new Term();
                      t.setType("document_type");
                      String term;
                      int type_id;
                      if (temp_val == "696" || temp_val == "ArchivalResource"){
                        type_id = 696;
                        t.setID(type_id);
                        term = "ArchivalResource";
                        break;
                      } else if (temp_val == "697" || temp_val == "BibliographicResource"){
                        type_id = 697;
                        t.setID(type_id);
                        term = "BibliographicResource";
                        break;
                      } else if (temp_val == "400479" || temp_val == "DigitalArchivalResource"){
                        type_id = 400479;
                        t.setID(type_id);
                        term = "DigitalArchivalResource";
                        break;
                      } else {
                        term = "";
                      }
                      t.setTerm(term);
                      res.setDocumentType(t);
                      // System.out.println("Type: " + temp_val);
                      break;
                  }
                  catch (NumberFormatException e){
                      System.out.println(temp_val + " is not a valid integer.");
                      break;
                  }
              case "title":
                  res.setTitle(temp_val);
                  // System.out.println("Title: " + temp_val);
                  break;
              case "display entry":
                  res.setDisplayEntry(temp_val);
                  // System.out.println("Display Entry: " + temp_val);
                  break;
              case "link":
                  res.setLink(temp_val);
                  // System.out.println("Link: " + temp_val);
                  break;
              case "abstract":
                  res.setAbstract(temp_val);
                  // System.out.println("Abstract: " + temp_val);
                  break;
              case "extent":
                  res.setExtent(temp_val);
                  // System.out.println("Extent: " + temp_val);
                  break;
              case "date":
                  res.setDate(temp_val);
                  // System.out.println("Date: " + temp_val);
                  break;
              case "language":
                  // Call language detecting function if not in dictionary of languages (cache)
                  temp_val = temp_val.toLowerCase();
                  String lang_term;
                  if (!language_code.containsKey(temp_val)){
                    lang_term = detectLanguage(temp_val);
                    if (lang_term == null){
                      break;
                    }
                  } else{
                    lang_term = temp_val;
                  }
                  String[] val_array = language_code.get(lang_term);

                  Language lang = new Language();
                  Term t = new Term();
                  t.setType("language_code");
                  try{
                    t.setID(Integer.parseInt(val_array[0]));
                  } catch (NumberFormatException e){
                    break;
                  }
                  t.setDescription(val_array[1]);
                  t.setTerm(lang_term);

                  lang.setLanguage(t);
                  res.addLanguage(lang);
                  // System.out.println("Language: " + lang_term);
                  break;
              case "holding repository snac id":
                  Constellation cons = new Constellation();
                  try {
                    cons.setID(Integer.parseInt(temp_val));
                  } catch(NumberFormatException e){
                    break;
                  }
                  res.setRepository(cons);
                  // System.out.println("HRSID: " + temp_val);
                  break;
              // case "note":
              //     System.out.println("Note: " + temp_val);
              default:
                  continue;
            }
        }
        return res;
    }

    /**
    * Helps determine whether a given ISO language exists on the SNAC database
    *
    * @param lang (ISO language code)
    * @return lang_term or null (ISO language code found in API Request)
    */
    public String detectLanguage(String lang){
        // Insert API request calls for lang (if exists: insert into language dict, if not: return None)
        try{
          DefaultHttpClient client = new DefaultHttpClient();
          HttpPost post = new HttpPost("http://snac-dev.iath.virginia.edu/api/");
          String query = "{\"command\": \"vocabulary\",\"query_string\": \"" + lang + "\",\"type\": \"language_code\",\"entity_type\": null}";
          post.setEntity(new StringEntity(query,"UTF-8"));
          HttpResponse response = client.execute(post);
          String result = EntityUtils.toString(response.getEntity());
          JSONParser jp = new JSONParser();
          JSONArray json_result = (JSONArray)((JSONObject)jp.parse(result)).get("results");
          if (json_result.size() <= 0){
            return null;
          }
          else{
            JSONObject json_val = (JSONObject)json_result.get(0);
            String lang_id = (String)json_val.get("id");
            String lang_desc = (String)json_val.get("description");
            String lang_term = (String)json_val.get("term");
            String[] lang_val = {lang_id, lang_desc};
            language_code.put(lang_term, lang_val);
            return lang_term;
          }
        }
        catch(IOException e){
          return null;
        }
        catch(ParseException e){
          return null;
        }
    }


    /**
    * Converts Project rows to Resources and store into Resources array
    *
    * @param none
    */
    public void rowsToResources(){
        // Clear LinkedList before adding resources
        resources.clear();
        List<Row> rows = theProject.rows;
        for (int x = 0; x < rows.size(); x++){
          Resource temp = createResource(rows.get(x));
          resources.add(temp);
        }
        // Resource temp = createResource(rows.get(rows.size()-1));
        // System.out.println();
        // System.out.println(Resource.toJSON(temp));
    }

    /**
    * Converts Resource Array into one JSON Object used to export
    *
    * @param none
    * @return String (String converted JSONObject of Resource Array)
    */

    public String exportResourcesJSON(){
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();
        JSONParser jp = new JSONParser();
        for (int x = 0; x < resources.size(); x++){
          try{
              ja.add((JSONObject)jp.parse(Resource.toJSON(resources.get(x))));
          }
          catch (ParseException e){
            continue;
          }
        }
        jo.put("resources", ja);
        return jo.toString();
        
    }

    public void uploadResources(String apiKey) {
    //    String apiKey = "NmZjMTY3Yjc4ZjgxZGRmMzM5YTI0YzZhMDVhMGJhNjE3MTU2ZTg5Mw";
    try{
        String opIns = ",\n\"operation\":\"insert\"\n},\"apikey\":\"" + apiKey +"\"";
        List<Resource> new_list_resources = new LinkedList<Resource>();
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://snac-dev.iath.virginia.edu/api/");
        for(Resource temp_res : resources){
            String rtj = Resource.toJSON(temp_res);
              String api_query = "{\"command\": \"insert_resource\",\n \"resource\":" + rtj.substring(0,rtj.length()-1) + opIns + "}";
              System.out.println("\n\n" + api_query + "\n\n");
              StringEntity casted_api = new StringEntity(api_query,"UTF-8");
              post.setEntity(casted_api);
              HttpResponse response = client.execute(post);
              String result = EntityUtils.toString(response.getEntity());
              //System.out.println("RESULT:" + result);
              new_list_resources.add(insertID(result,temp_res));
          }
          resources = new_list_resources;
          System.out.println("Uploading Attempt Complete");
        }catch(IOException e){
          System.out.println(e);
        }
    }

    public Resource insertID(String result, Resource res){
        JSONParser jp = new JSONParser();
        try{
            JSONObject jsonobj = (JSONObject)jp.parse(result);
            int new_id = Integer.parseInt((((JSONObject)jsonobj.get("resource")).get("id")).toString());
            if(new_id!=0){
              resource_ids.add(new_id);
              res.setID(new_id);
              return res;
            }
            else{
              resource_ids.add(null);
            }
        }
        catch (ParseException e){
            System.out.println(e);
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println("Hello");

    }
}
