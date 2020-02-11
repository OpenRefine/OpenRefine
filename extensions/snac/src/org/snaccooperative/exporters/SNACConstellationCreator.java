package org.snaccooperative.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
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
  - "term": "Archival constellation"
    - getTerm()
    - setTerm(String)
  - "type": "document_type"
    - getType()
    - setType(String)
Constellation Object
*/

public class SNACConstellationCreator {
    public static HashMap<String, String> match_attributes = new HashMap<String, String>();
    private static Project theProject = new Project();
    private static final SNACConstellationCreator instance = new SNACConstellationCreator();
    private static List<Constellation> constellations = new LinkedList<Constellation>();
    public static List<String> csv_headers = new LinkedList<String>();

    // Internal constellation IDs that isn't part of the constellation data model
    public static List<Integer> constellation_ids = new LinkedList<Integer>();
    private static HashMap<String, String[]> language_code = new HashMap<String, String[]>();

    public static SNACConstellationCreator getInstance() {
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
        // System.out.println(new JSONObject(match_attributes).toString());
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
        rowsToConstellations();
        exportConstellationsJSON();
    }


    /**
    * Take a given Row and convert it to a constellation Object
    *
    * @param row (Row found in List<Row> from Project)
    * @return constellation converted from Row
    */
    public Constellation createConstellation(Row row){
        Constellation con = new Constellation();
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
                      con.setID(Integer.parseInt(temp_val));
                      constellation_ids.add(Integer.parseInt(temp_val));
                      System.out.println("ID: " + temp_val);
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
                      if (temp_val.equals("696") || temp_val.equals("ArchivalConstellation")){
                        type_id = 696;
                        t.setID(type_id);
                        term = "Archivalconstellation";
                      } else if (temp_val.equals("697") || temp_val.equals("BibliographicConstellation")){
                        type_id = 697;
                        t.setID(type_id);
                        term = "Bibliographicconstellation";
                      } else if (temp_val.equals("400479") || temp_val.equals("DigitalArchivalConstellation")){
                        type_id = 400479;
                        t.setID(type_id);
                        term = "DigitalArchivalConstellation";
                      } else {
                        throw new NumberFormatException();
                      }
                      t.setTerm(term);
                      //con.setDocumentType(t);
                      break;
                  }
                  catch (NumberFormatException e){
                      System.out.println(temp_val + " is not a valid constellation type.");
                      break;
                  }
                  catch (Exception e){
                    System.out.println(e);
                    break;
                  }
              case "name entry":
                  //con.setTitle(temp_val);
                  //System.out.println("Title: " + temp_val);
                  break;
              case "date":
                  //con.setDisplayEntry(temp_val);
                  //System.out.println("Display Entry: " + temp_val);
                  break;
              case "subject":
                  //con.setLink(temp_val);
                  //System.out.println("Link: " + temp_val);
                  break;
              case "place":
                  //con.setAbstract(temp_val);
                  //System.out.println("Abstract: " + temp_val);
                  break;
              case "occupation":
                  //con.setExtent(temp_val);
                  //System.out.println("Extent: " + temp_val);
                  break;
              case "function":
                  //con.setDate(temp_val);
                  //System.out.println("Date: " + temp_val);
                  break;
              // case "language":
              //     Language lang = new Language();
              //     Term t = new Term();
              //     t.setType(temp_val);
              //     lang.setLanguage();
              //     r.addLanguage(lang);
              //     System.out.println("Language: " + temp_val);
              //     break;
              case "blog history":
                  // System.out.println("HRSID: " + temp_val);
                  Constellation cons = new Constellation();
                  // Insert ID into cons (WORK IN PROGconS)
                  //con.setRepository(cons);
                  // System.out.println("conult: " + Integer.toString(con.getRepository().getID()));
                  break;
              default:
                  continue;
            }
        }
        return con;
    }

    /**
    * Converts 2 constellations into format for Preview Tab
    *
    * @param none
    * @return String
    */
    public String obtainPreview(){
      String samplePreview = "";
      if (constellations.size() == 0){
        samplePreview += "There are no constellations to preview.";
      }
      else{
        samplePreview += "Inserting " + constellations.size() + " new constellations into SNAC." + "\n";

        int iterations = Math.min(constellations.size(), 2);

        for(int x = 0; x < iterations; x++){
          Constellation previewConstellation = constellations.get(x);

          for(Map.Entry mapEntry: match_attributes.entrySet()){
              if(!((String)mapEntry.getValue()).equals("")){
                switch((String)mapEntry.getKey()) {
                  case "id":
                    samplePreview+= "ID: " + previewConstellation.getID() + "\n";
                    break;
                  // case "type":
                  //   Term previewTerm = previewConstellation.getDocumentType();
                  //   samplePreview+="Document Type: " + previewTerm.getTerm() + " (" + previewTerm.getID() +")\n";
                  //   break;
                  // case "title":
                  //   samplePreview+="Title: " + previewConstellation.getTitle() + "\n";
                  //   break;
                  // case "display_entry":
                  //   samplePreview+="Display Entry: " + previewConstellation.getDisplayEntry() + "\n";
                  //   break;
                  // case "link":
                  //   samplePreview+="Link: " + previewConstellation.getLink() + "\n";
                  //   break;
                  // case "abstract":
                  //   samplePreview+="Abstract: " + previewConstellation.getAbstract() + "\n";
                  //   break;
                  // case "extent":
                  //   samplePreview+="Extent: " + previewConstellation.getExtent() +  "\n";
                  //   break;
                  // case "date":
                  //   samplePreview+="Date: " + previewConstellation.getDate() + "\n";
                  //   break;
                  // case "lang":
                  //   List<Language> languageList = previewConstellation.getLanguages();
                  //   String previewConstellationLanguages = "Language(s): ";
                  //   if(languageList.size() == 0){
                  //     previewConstellationLanguages = "Language(s): " + "\n" ;
                  //   }
                  //   for(int i=0; i<languageList.size();i++){
                  //     if(languageList.size() == 0){
                  //       break;
                  //     }
                  //     if(i != languageList.size()-1){
                  //       previewConstellationLanguages+=languageList.get(i).getLanguage().getTerm().toString() +", ";
                  //     }
                  //     else{
                  //       previewConstellationLanguages+=languageList.get(i).getLanguage().getTerm().toString() + "\n";
                  //     }
                  //   }
                  //   samplePreview+= previewConstellationLanguages;
                  //   break;
                  // case "repo_ic_id":
                  //   try{
                  //     samplePreview+="Repository ID: "+ Integer.toString(previewConstellation.getRepository().getID()) + "\n";
                  //   } catch (NullPointerException e){
                  //     samplePreview+="Repository ID: " + "\n";
                  //   }
                  //   break;
                  default:
                    break;
                }
              }
          }
        }
      }
      // System.out.println(samplePreview);
      return samplePreview;

    }

    /*
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
    * Converts Project rows to constellations and store into constellations array
    *
    * @param none
    */
    public void rowsToConstellations(){
        // Clear LinkedList before adding constellations
        constellations.clear();
        List<Row> rows = theProject.rows;
        for (int x = 0; x < rows.size(); x++){
          Constellation temp = createConstellation(rows.get(x));
          constellations.add(temp);
        }
        // constellation temp = createconstellation(rows.get(rows.size()-1));
        // System.out.println();
        // System.out.println(constellation.toJSON(temp));
    }

    /**
    * Converts constellation Array into one JSON Object used to export
    *
    * @param none
    * @return String (String converted JSONObject of constellation Array)
    */
    public String exportConstellationsJSON(){
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();
        JSONParser jp = new JSONParser();
        for (int x = 0; x < constellations.size(); x++){
          try{
              ja.add((JSONObject)jp.parse(Constellation.toJSON(constellations.get(x))));
          }
          catch (ParseException e){
            continue;
          }
        }
        jo.put("constellations", ja);
        //System.out.println(jo.toString());
        return jo.toString();

    }

    public void uploadConstellations(String apiKey) {

    // try{
    //     String opIns = ",\n\"operation\":\"insert\"\n},\"apikey\":\"" + apiKey +"\"";
    //     List<constellation> new_list_constellations = new LinkedList<constellation>();
    //     DefaultHttpClient client = new DefaultHttpClient();
    //     HttpPost post = new HttpPost("http://snac-dev.iath.virginia.edu/api/");
    //     System.out.println("Querying SNAC...");
    //     for(constellation temp_con : constellations){
    //         String rtj = constellation.toJSON(temp_con);
    //           String api_query = "{\"command\": \"insert_constellation\",\n \"constellation\":" + rtj.substring(0,rtj.length()-1) + opIns + "}";
    //           // System.out.println("\n\n" + api_query + "\n\n");
    //           StringEntity casted_api = new StringEntity(api_query,"UTF-8");
    //           post.setEntity(casted_api);
    //           Httpconponse conponse = client.execute(post);
    //           String conult = EntityUtils.toString(conponse.getEntity());
    //           //System.out.println("conULT:" + conult);
    //           new_list_constellations.add(insertID(conult,temp_con));
    //       }
    //       constellations = new_list_constellations;
    //       System.out.println("Uploading Attempt Complete");
    //     }catch(IOException e){
    //       System.out.println(e);
    //     }
    // }

//     public constellation insertID(String conult, constellation con){
//     JSONParser jp = new JSONParser();
//     try{
//         JSONObject jsonobj = (JSONObject)jp.parse(conult);
//         int new_id = Integer.parseInt((((JSONObject)jsonobj.get("constellation")).get("id")).toString());
//         if(new_id!=0){
//           constellation_ids.add(new_id);
//           con.setID(new_id);
//           return con;
//         }
//         else{
//           constellation_ids.add(null);
//         }
//     }
//     catch (ParseException e){
//         System.out.println(e);
//     }
//     return con;
  }
}
