package com.google.gridworks.templating;

import com.google.gridworks.expr.Evaluable;

class DynamicFragment extends Fragment {
	final public Evaluable eval;
	
	public DynamicFragment(Evaluable eval) {
		this.eval = eval;
	}
}
