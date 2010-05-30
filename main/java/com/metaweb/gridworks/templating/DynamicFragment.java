package com.metaweb.gridworks.templating;

import com.metaweb.gridworks.expr.Evaluable;

class DynamicFragment extends Fragment {
	final public Evaluable eval;
	
	public DynamicFragment(Evaluable eval) {
		this.eval = eval;
	}
}
