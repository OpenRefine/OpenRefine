package org.deri.grefine.reconcile.rdf.factories;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class PreviewResourceCannedQuery {

	private final String query; 
	
	public PreviewResourceCannedQuery(InputStream properties) throws IOException{
		Properties props = new Properties();
		props.load(properties);
		
		String[] labels = tokenize(props.getProperty("label"));
		String[] descriptions = tokenize(props.getProperty("description"));
		String[] images = tokenize(props.getProperty("image"));
		
		StringBuilder selectClause = new StringBuilder();
		StringBuilder whereClause = new StringBuilder();
		
		for(int i=0; i<labels.length;i++){
			String uri = labels[i].trim();
			selectClause.append(" ?en_label").append(i).append(" ?label").append(i);
			whereClause.append("OPTIONAL { <[[RESOURCE_URI]]> <").append(uri).append("> ?en_label").append(i).append(" FILTER langMatches(lang(?en_label").append(i).append("),'en') } ");
			whereClause.append("OPTIONAL { <[[RESOURCE_URI]]> <").append(uri).append("> ?label").append(i).append(" FILTER langMatches(lang(?label").append(i).append("),'') } ");
		}
		
		for(int i=0; i<descriptions.length;i++){
			String uri = descriptions[i].trim();
			selectClause.append(" ?en_desc").append(i).append(" ?desc").append(i);
			whereClause.append("OPTIONAL { <[[RESOURCE_URI]]> <").append(uri).append("> ?en_desc").append(i).append(" FILTER langMatches(lang(?en_desc").append(i).append("),'en') } ");
			whereClause.append("OPTIONAL { <[[RESOURCE_URI]]> <").append(uri).append("> ?desc").append(i).append(" FILTER langMatches(lang(?desc").append(i).append("),'') } ");
		}
		
		for(int i=0; i<images.length;i++){
			String uri = images[i].trim();
			selectClause.append(" ?img").append(i);
			whereClause.append("OPTIONAL { <[[RESOURCE_URI]]> <").append(uri).append("> ?img").append(i).append("} ");
		}
		
		query = "SELECT" + selectClause + " WHERE{ " + whereClause + "}LIMIT 1" ;
	}
	
	public String getPreviewQueryForResource(String resourceId){
		return query.replaceAll("\\[\\[RESOURCE_URI\\]\\]", resourceId);
	}
	
	public Multimap<String, String> wrapResourcePropertiesMapResultSet(ResultSet resultset){
		Multimap<String, String> map = LinkedHashMultimap.create();
		List<String> vars = resultset.getResultVars();
		while(resultset.hasNext()){
			QuerySolution sol = resultset.nextSolution();
			List<String> labels = getFirstBoundN(sol,vars,"label",1,true);
			List<String> descriptions = getFirstBoundN(sol,vars,"desc",1,true);
			List<String> images = getFirstBoundN(sol,vars,"img",1,false);
			map.putAll("labels", labels);
			map.putAll("descriptions", descriptions);
			map.putAll("images", images);
		}
		return map;
	}
	
	private String[] tokenize(String s){
		return StringUtils.split(s, ",");
	}
	
	private List<String> getFirstBoundN(QuerySolution sol, List<String> varNames, String varName, int num, boolean langAware){
		List<String> values = new ArrayList<String>(num);
		String name, en_name;
		int i = 0;
		String prefix = langAware? "en_":"";
		do{
			name = varName + i;
			en_name = prefix + name;
			i+=1;
			RDFNode val = sol.get(en_name);
			if(val==null){
				if(!langAware){
					continue;
				}
				//if langAware, try the one without language tag
				val = sol.get(name);
				if(val==null){
					continue;
				}
			}
			values.add(getString(val));
			if(values.size()==num){
				break;
			}
		}while(varNames.contains(name));
		
		return values;
	}
	
	private String getString(RDFNode node){
		if(node.canAs(Literal.class)){
			return node.asLiteral().getString();
		}else if (node.canAs(Resource.class)){
			return node.asResource().getURI();
		}
		return node.toString();
	}
}
