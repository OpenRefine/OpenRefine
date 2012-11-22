package com.google.refine.crowdsourcing.crowdflower;

//TODO: does it make sense to implement an importer to import data *from* CF?

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.crowdsourcing.CrowdsourcingUtil;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zemanta.crowdflower.client.CrowdFlowerClient;

public class CreateNewJobCommand extends Command{
    static final Logger logger = LoggerFactory.getLogger("crowdflower_createnewjob");
    protected List<Integer> _cell_indeces;
    
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            JSONObject extension = new JSONObject(request.getParameter("extension")); 

            String apiKey = (String) CrowdsourcingUtil.getPreference("crowdflower.apikey");                       
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            
            System.out.println("Project id: " + project.id);
            
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
            //JSONObject results;
            String response_msg = "";
            String status = "";
            
            String job_title = extension.getString("title");
            String job_instructions = extension.getString("instructions");
            String job_id = "0";
            
            
            if(extension.has("new_job") && extension.getBoolean("new_job")) {
                //new empty job with title and instructions
                response_msg = cf_client.createNewJobWithoutData(job_title, job_instructions);

                JSONObject obj =  new JSONObject(response_msg);
                status = obj.getString("status");
                System.out.println("Status - creating job: " + status);
                
                if(obj != null && obj.has("response")) {
                    //get job id
                    JSONObject obj2 = obj.getJSONObject("response");
                    if(obj2.has("id")) {
                        job_id = obj2.getString("id");
                        System.out.println("Job id: " + job_id);
                    }

                }
            }
            else if(extension.has("id")) {
                job_id = extension.getString("id");
            }
            else {
                //ERROR
                System.out.println("Missing job id!");
            }
            
            
            if(extension.getBoolean("upload")) {
                
                System.out.println("Generating objects for upload....");
                _cell_indeces = new ArrayList<Integer>();
                JSONArray column_names = extension.getJSONArray("column_names"); 
               
                for (int i=0; i < column_names.length(); i++) {
                    Column col = project.columnModel.getColumnByName(column_names.get(i).toString());
                    _cell_indeces.add(col.getCellIndex());
                }

                FilteredRows rows = engine.getAllFilteredRows();
                List<Integer> rows_indeces = new ArrayList<Integer>();
                
                
                //for each row create JSON object
                rows.accept(project, new RowVisitor() {

                    List<Integer> _rowIndices;
                    
                    public RowVisitor init(List<Integer> rowIndices) {
                        _rowIndices = rowIndices;
                        return this;
                    }
                    
                    @Override
                    public void start(Project project) {
                        // nothing to do 
                    }

                    @Override
                    public void end(Project project) {
                        // nothing to do
                        
                    }
                    
                    @Override
                    public boolean visit(Project project, int rowIndex, Row row) {
                        //check cells in any of the selected columns
                        
                        for (int k = 0; k < _cell_indeces.size(); k ++) {
                        
                            Cell cell = row.getCell(_cell_indeces.get(k));
                            if(cell != null) { //as soon a value is encountered in any of selected columns, add row index
                                _rowIndices.add(rowIndex);
                                break;
                            }
                        }
                        return false;
                    }}.init(rows_indeces));
                
                
                StringBuffer bf = new StringBuffer();
                
                for(Iterator<Integer> it = rows_indeces.iterator(); it.hasNext();) {
                    int row_index = it.next();
                    
                    JSONObject obj =  new JSONObject();
                    for (int c=0; c < column_names.length(); c++) {
                        int cell_index = _cell_indeces.get(c);
                        String key = column_names.get(c).toString();
                        Object value = project.rows.get(row_index).getCellValue(cell_index);
                        
                        obj.put(key, value.toString());
                    } 
                    
                    System.out.println("Data: " + obj.toString());
                   bf.append(obj.toString()); 
                    
                }
             
                response_msg = cf_client.bulkUploadJSONToExistingJob(job_id, bf.toString());
                
                System.out.println(response_msg);
            }
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {
                writer.object();
                writer.key("status"); writer.value(status);
                writer.key("job_id"); writer.value(job_id);    
            } catch(Exception e){
                logger.error("New job not created.");
            }
            finally {
                writer.endObject();
                w.flush();
                w.close();
            }
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage(),e);
        }
        
    }

    
}
