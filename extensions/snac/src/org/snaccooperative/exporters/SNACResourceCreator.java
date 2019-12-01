package org.snaccooperative.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

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
    public static List<Integer> resource_ids = new LinkedList<Integer>();

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
                      System.out.println("ID: " + temp_val);
                      break;
                  }
                  catch (NumberFormatException e){
                      break;
                  }
              case "type":
                  try{
                      int type_id = Integer.parseInt(temp_val);
                      Term t = new Term();
                      t.setType("document_type");
                      t.setID(type_id);
                      String term;
                      switch(type_id){
                          case 696:
                              term = "ArchivalResource";
                              break;
                          case 697:
                              term = "BibliographicResource";
                              break;
                          case 400479:
                              term = "DigitalArchivalResource";
                              break;
                          default:
                              term = "";
                      }
                      t.setTerm(term);
                      res.setDocumentType(t);
                      System.out.println("Type: " + temp_val);
                      break;
                  }
                  catch (NumberFormatException e){
                      System.out.println(temp_val + " is not a valid integer.");
                      break;
                  }
              case "title":
                  res.setTitle(temp_val);
                  System.out.println("Title: " + temp_val);
                  break;
              case "display entry":
                  res.setDisplayEntry(temp_val);
                  System.out.println("Display Entry: " + temp_val);
                  break;
              case "link":
                  res.setLink(temp_val);
                  System.out.println("Link: " + temp_val);
                  break;
              case "abstract":
                  res.setAbstract(temp_val);
                  System.out.println("Abstract: " + temp_val);
                  break;
              case "extent":
                  res.setExtent(temp_val);
                  System.out.println("Extent: " + temp_val);
                  break;
              case "date":
                  res.setDate(temp_val);
                  System.out.println("Date: " + temp_val);
                  break;
              // case "language":
              //     Language lang = new Language();
              //     Term t = new Term();
              //     t.setType(temp_val);
              //     lang.setLanguage();
              //     r.addLanguage(lang);
              //     System.out.println("Language: " + temp_val);
              //     break;
              case "holding repository snac id":
                  Constellation cons = new Constellation();
                  // Insert ID into cons (WORK IN PROGRESS)
                  res.setRepository(cons);
                  System.out.println("HRSID: " + temp_val);
                  break;
              case "note":
                  System.out.println("Note: " + temp_val);
              default:
                  continue;
            }
        }
        return res;
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
    public void exportResourcesJSON(){
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
        System.out.println(jo.toString());
    }

    public void pt4de(){

    }

    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
