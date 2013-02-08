package org.deri.grefine.reconcile.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.deri.grefine.reconcile.sindice.SindiceBroker;
import org.deri.grefine.reconcile.util.GRefineJsonUtilitiesImpl;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class SindiceGuessTypeCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			Project project = getProject(request);
			String columnName = request.getParameter("columnName");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Content-Type", "application/json");

			JSONWriter writer = new JSONWriter(response.getWriter());
			writer.object();

			Column column = project.columnModel.getColumnByName(columnName);
			if (column == null) {
				writer.key("code");
				writer.value("error");
				writer.key("message");
				writer.value("No such column");
			} else {
				try {
					writer.key("code");
					writer.value("ok");
					List<String> domains = guessDomain(project, column);
					writer.key("domains");
					writer.array();
					for(String domain:domains){
						writer.value(domain);
					}
					writer.endArray();
				} catch (Exception e) {
					writer.key("code");
					writer.value("error");
				}
			}

			writer.endObject();
		} catch (Exception e) {
			respondException(response, e);
		}
	}
	
	final static int s_sampleSize = 10;
	final static int s_resultsLimit = 3;
	
	private List<String> guessDomain(Project project, Column column){
		Map<String,Integer> domainsMap = new HashMap<String, Integer>();

        int cellIndex = column.getCellIndex();
        
        List<String> samples = new ArrayList<String>(s_sampleSize);
        Set<String> sampleSet = new HashSet<String>();
        
        for (Row row : project.rows) {
            Object value = row.getCellValue(cellIndex);
            if (ExpressionUtils.isNonBlankData(value)) {
                String s = value.toString().trim();
                if (!sampleSet.contains(s)) {
                    samples.add(s);
                    sampleSet.add(s);
                    if (samples.size() >= s_sampleSize) {
                        break;
                    }
                }
            }
        }
        
        SindiceBroker service = new SindiceBroker();
        for(int j=0;j<samples.size();j++){
        	String s = samples.get(j);
        	List<String> domains = service.guessDomain(s,s_resultsLimit,new GRefineJsonUtilitiesImpl());
        	for(String domain:domains){
        		if(domainsMap.containsKey(domain)){
        			domainsMap.put(domain,domainsMap.get(domain).intValue() + 1);
        		}else{
        			domainsMap.put(domain, 0);
        		}
        	}
        }
        
        List<Entry<String, Integer>> domainsEntries = new LinkedList<Entry<String, Integer>>(domainsMap.entrySet());
        Collections.sort(domainsEntries, new Comparator<Entry<String, Integer>>() {
             public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
            	 return o2.getValue().compareTo(o1.getValue());
             }
        });
        
        List<String> domains = new ArrayList<String>();
        for(Entry<String, Integer> entry: domainsEntries){
        	domains.add(entry.getKey());
        }
        return domains;
	}
}
