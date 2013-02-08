package org.deri.grefine.reconcile.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deri.grefine.reconcile.model.SearchResultItem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class ResultSetWrappingUtil {

	public static ImmutableList<SearchResultItem> resultSetToSearchResultList(ResultSet resultSet){
		List<String> varNames = resultSet.getResultVars();
		String idVar = varNames.get(0);
		String labelVar = varNames.get(1);
		String scoreVar = null;
		if(varNames.size()>2){
			scoreVar = varNames.get(2);
		}
		List<SearchResultItem> results = new ArrayList<SearchResultItem>();
		while(resultSet.hasNext()){
			QuerySolution sol = resultSet.next();
			Literal nameLiteral = sol.getLiteral(labelVar);
			String name = nameLiteral==null?"":nameLiteral.getString();
			String id = sol.getResource(idVar).getURI();
			double score = 0;
			if(scoreVar!=null){
				score = sol.getLiteral(scoreVar).getDouble();
			}
			
			results.add(new SearchResultItem(id, name, score));
		}
		return ImmutableList.copyOf(results);
	}
	
	public static ImmutableList<SearchResultItem> resultSetToSearchResultListFilterDuplicates(ResultSet resultSet, int limit){
		List<String> varNames = resultSet.getResultVars();
		String idVar = varNames.get(0);
		String labelVar = varNames.get(1);
		String scoreVar = null;
		if(varNames.size()>2){
			scoreVar = varNames.get(2);
		}
		List<SearchResultItem> results = new ArrayList<SearchResultItem>();
		Set<String> seen = new HashSet<String>();
		while(resultSet.hasNext()){
			QuerySolution sol = resultSet.next();
			String id = sol.getResource(idVar).getURI();
			if(seen.contains(id)){
				continue;
			}
			seen.add(id);
			Literal nameLiteral = sol.getLiteral(labelVar);
			String name = nameLiteral==null?"":nameLiteral.getString();
			double score = 0;
			if(scoreVar!=null){
				score = sol.getLiteral(scoreVar).getDouble();
			}
			
			results.add(new SearchResultItem(id, name, score));
			if(results.size()==limit){
				//got enough
				break;
			}
		}
		return ImmutableList.copyOf(results);
	}
	
	/**
	 * @param resultSet
	 * @param limit number of unique keys to include in the result
	 * @return LinkedHashMultimap keeps the order of key input, so order in the result set is retained
	 * the first variable in resultSet solutions is the key to the map ,the second is the value
	 */
	public static LinkedHashMultimap<String, String> resultSetToMultimap(ResultSet resultSet){
		LinkedHashMultimap<String,String> map = LinkedHashMultimap.create();
		List<String> varNames = resultSet.getResultVars();
		if(varNames.size()!=2){
			throw new RuntimeException("resultSetToMultimap only accepts a resultset with exactly two variables in the solution");
		}
		String keyVar = varNames.get(0);
		String valVar = varNames.get(1);
		while(resultSet.hasNext()){
			QuerySolution sol = resultSet.next();
			
			String key = sol.getResource(keyVar).getURI();
			RDFNode valNode = sol.get(valVar);
			String val = valNode.canAs(Resource.class)?valNode.as(Resource.class).getURI():valNode.asLiteral().getString();
			/*if(map.keySet().size()==limit && !map.keySet().contains(key)){
				break;
			}*/
			map.put(key, val);
		}
		return map;
	}
	
	public static List<String> resultSetToList(ResultSet resultset, String varName){
		List<String> result = new ArrayList<String>();
		while(resultset.hasNext()){
			QuerySolution sol = resultset.next();
			String uri = sol.getResource(varName).getURI();
			result.add(uri);
		}
		return result;
	}
	
	public static ImmutableList<String[]> resultSetToListOfPairs(ResultSet resultset){
		List<String> varNames = resultset.getResultVars();
		String var1 = varNames.get(0);
		String var2 = varNames.get(1);
		List<String[]> result = new ArrayList<String[]>();
		while(resultset.hasNext()){
			QuerySolution sol = resultset.next();
			String s = sol.getResource(var1).getURI();
			RDFNode object = sol.get(var2);
			/*String o;
			if(object.canAs(Resource.class)){
				o = object.asResource().getURI();
			}else{
				o = object.asLiteral().getString();
			}*/
			result.add(new String[]{s,object.toString()});
		}
		return ImmutableList.copyOf(result);
	}
}
