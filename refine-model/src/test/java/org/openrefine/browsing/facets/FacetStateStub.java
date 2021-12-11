package org.openrefine.browsing.facets;

/**
 * A simple facet state which simply computes the number
 * of matching and mismatching rows.
 */
public class FacetStateStub implements FacetState, FacetResult {
	
	private static final long serialVersionUID = 1L;
	protected int matching;
	protected int mismatching;
	
	public FacetStateStub(int matching, int mismatching) {
		this.matching = matching;
		this.mismatching = mismatching;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof FacetStateStub)) {
			return false;
		}
		FacetStateStub otherStub = (FacetStateStub) other;
		return (otherStub.matching == matching
				&& otherStub.mismatching == mismatching);
	}
	
	@Override
	public String toString() {
		return String.format("[FacetStateStub: %d, %d]", matching, mismatching);
	}
	
}