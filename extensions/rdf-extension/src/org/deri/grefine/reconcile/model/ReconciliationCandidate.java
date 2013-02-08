package org.deri.grefine.reconcile.model;

public class ReconciliationCandidate {

	private final String id;
	private final String name;
	private String[] types;
	private final double score;
	private boolean match;
	
	public ReconciliationCandidate(String id, String name, String[] types, double score, boolean match){
		this.id = id;
		this.name = name;
		this.types = types;
		this.score = score;
		this.match = match;
	}

	public String getId() {
		return id;
	}

	public boolean isMatch() {
		return match;
	}

	public void setMatch(boolean match) {
		this.match = match;
	}

	public String getName() {
		return name;
	}

	public String[] getTypes() {
		return types;
	}

	public double getScore() {
		return score;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public void setTypes(String[] types) {
		this.types = types;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(!obj.getClass().equals(this.getClass())) return false;
		return this.getId().equals(((ReconciliationCandidate)obj).getId());
	}
	
	
}
