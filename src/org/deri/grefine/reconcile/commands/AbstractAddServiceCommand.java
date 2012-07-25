package org.deri.grefine.reconcile.commands;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.reconcile.model.ReconciliationService;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.google.refine.commands.Command;

public abstract class AbstractAddServiceCommand extends Command{

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			ReconciliationService service = getReconciliationService(request);
			response.setCharacterEncoding("UTF-8");
	        response.setHeader("Content-Type", "application/json");

	        Writer w = response.getWriter();
	        JSONWriter writer = new JSONWriter(w);
	        
	        writer.object();
	        writer.key("code"); writer.value("ok");
	        writer.key("service");
	        service.writeAsJson(writer);
	        writer.endObject();
	        w.flush();
	        w.close();
		} catch (Exception e) {
			respondException(response, e);
		}
	}

	protected String getIdForString(String name){
		return name.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^-.a-zA-Z0-9]", "").replaceAll("\\-\\-+", "-");
	}
	
	protected ImmutableList<String> asImmutableList(String text){
		List<String> lst = new ArrayList<String>();
		if (StringUtils.isNotBlank(text)) {
			StringTokenizer tokenizer = new StringTokenizer(text," \n");
			while(tokenizer.hasMoreTokens()){
				String token = tokenizer.nextToken();
				if(token.trim().isEmpty()){
					continue;
				}
				lst.add(token.trim());
			}
		}
		return ImmutableList.copyOf(lst);
	}
	
	protected abstract ReconciliationService getReconciliationService(HttpServletRequest request)throws JSONException, IOException;	
}
