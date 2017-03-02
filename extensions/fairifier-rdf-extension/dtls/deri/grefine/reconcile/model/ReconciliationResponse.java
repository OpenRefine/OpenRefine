package org.deri.grefine.reconcile.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReconciliationResponse {

	private List<? extends ReconciliationCandidate> results;
	
	public ReconciliationResponse(){
		this.results = new ArrayList<ReconciliationCandidate>();
	}
	
	/*public void addResult(ReconciliationCandidate result){
		this.results.add(result);
	}*/

	public List<? extends ReconciliationCandidate> getResults() {
		return results;
	}

	public void setResults(List<? extends ReconciliationCandidate> results) {
		this.results = results;
	}
	
}
