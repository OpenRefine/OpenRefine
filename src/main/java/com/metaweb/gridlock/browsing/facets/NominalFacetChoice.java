package com.metaweb.gridlock.browsing.facets;

import com.metaweb.gridlock.browsing.accessors.DecoratedValue;

public class NominalFacetChoice {
	final public DecoratedValue	decoratedValue;
	final public Object			value;
	public int					count;
	
	public NominalFacetChoice(DecoratedValue decoratedValue, Object value) {
		this.decoratedValue = decoratedValue;
		this.value = value;
	}
}
