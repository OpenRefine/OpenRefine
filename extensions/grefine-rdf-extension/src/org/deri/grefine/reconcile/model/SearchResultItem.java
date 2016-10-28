package org.deri.grefine.reconcile.model;

/**
 * @author fadmaa
 * represents an item in search result
 */
/**
 * @author fadmaa
 *
 */
public class SearchResultItem {

	/**
	 * strong identifier
	 */
	private final String id;
	/**
	 * human friendly name
	 */
	private final String name;
	
	/**
	 * relevance of the search result item in [0..1]
	 */
	private final double score;
	
	public SearchResultItem(String id, String name){
		this(id,name,0);
	}
	
	public SearchResultItem(String id, String name, double score) {
		this.id = id;
		this.name = name;
		this.score = score;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getScore() {
		return score;
	}
	
}
